package com.bintech.metrix.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.dto.response.MarketWorkbenchResponse;
import com.bintech.metrix.dto.response.PortfolioHoldingVO;
import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.dao.PortfolioHoldingDao;
import com.bintech.metrix.repository.entity.PortfolioHolding;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.MarketWorkbenchService;
import com.bintech.metrix.service.PortfolioHoldingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 以同花顺金融数据 API 的快照和前复权日 K 构建工作台数据。
 *
 * <p>所有技术指标只由已成功返回的历史日线计算，不对上游缺失值补零。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketWorkbenchServiceImpl implements MarketWorkbenchService {

    private static final int HISTORY_COUNT = 252;
    private static final int HOLDING_LIMIT = 8;
    private static final Map<String, Integer> DRAWDOWN_PERIODS = Map.of(
            "maxDrawdown1M", 21, "maxDrawdown3M", 63,
            "maxDrawdown6M", 126, "maxDrawdown1Y", HISTORY_COUNT);
    private static final int AVERAGE_AMOUNT_DAYS = 20;
    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StockBasicDao stockBasicDao;
    private final PortfolioHoldingDao portfolioHoldingDao;
    private final MarketDataService marketDataService;
    private final PortfolioHoldingService portfolioHoldingService;

    @Override
    public MarketWorkbenchResponse load(String thscode, Long userId) {
        StockBasic stock = findStock(thscode);
        Map<String, Object> quoteResponse = marketDataService.fetchRealTimeData(stock, userId);
        Map<String, Object> klineResponse = marketDataService.fetchKlinesData(stock, HISTORY_COUNT, userId);
        JSONObject quote = firstQuote(quoteResponse);
        List<Map<String, Object>> bars = buildBars(klineResponse);
        if (bars.isEmpty()) {
            throw new IllegalStateException("同花顺金融数据 API 未返回有效前复权日K");
        }

        Map<String, Object> instrument = Map.of("thscode", stock.getTsCode(), "symbol", stock.getSymbol(), "name", stock.getName());
        String dataTime = formatDataTime(quote.getLong("timestamp", 0L));
        Map<String, Object> quoteData = quoteData(quote);
        Map<String, Object> metrics = calculateMetrics(bars);
        List<PortfolioHoldingVO> holdings = holdings();
        return new MarketWorkbenchResponse(instrument, quoteData, metrics, bars, holdings,
                "同花顺金融数据 API", "前复权", dataTime);
    }

    @Override
    public List<PortfolioHoldingVO> holdings() {
        return portfolioHoldingService.getHoldings(null, null).getHoldings().stream()
                .limit(HOLDING_LIMIT)
                .toList();
    }

    /** 按当前用户全部账户的持仓过滤异动，无持仓时不请求上游。 */
    @Override
    public Map<String, Object> anomalies(Long userId) {
        try {
            Set<String> holdingCodes = portfolioHoldingDao.selectByUserId(userId).stream()
                    .map(PortfolioHolding::getStockCode)
                    .filter(Objects::nonNull)
                    .map(code -> code.trim().toUpperCase(Locale.ROOT))
                    .filter(code -> !code.isBlank())
                    .collect(Collectors.toSet());
            if (holdingCodes.isEmpty()) {
                return Map.of("items", List.of(), "source", "同花顺金融数据 API");
            }
            JSONObject data = loadAnomalyData(userId);
            JSONArray items = data.getJSONArray("item");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("items", items.toList(JSONObject.class).stream()
                    .filter(item -> holdingCodes.contains(item.getStr("thscode", "").trim().toUpperCase(Locale.ROOT)))
                    .toList());
            result.put("dataTime", formatDataTime(data.getLong("timestamp", 0L)));
            result.put("source", "同花顺金融数据 API");
            return result;
        } catch (RuntimeException error) {
            log.warn("获取当日个股异动原因失败: {}", error.getMessage());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("items", List.of());
            result.put("source", "同花顺金融数据 API");
            result.put("error", error.getMessage());
            return result;
        }
    }

    /** 校验上游异动快照，保留原始字段和时间。 */
    private JSONObject loadAnomalyData(Long userId) {
        JSONObject payload = JSONUtil.parseObj(marketDataService.fetchAnomalyAnalysisData(userId));
        if (!ApiConstants.STATUS_SUCCESS.equals(payload.getStr(ApiConstants.KEY_STATUS))) {
            throw new IllegalStateException(payload.getStr(ApiConstants.KEY_MESSAGE, "个股异动原因获取失败"));
        }
        JSONObject data = payload.getJSONObject(ApiConstants.KEY_DATA);
        if (data == null || data.getJSONArray("item") == null) {
            throw new IllegalStateException("同花顺金融数据 API 未返回个股异动原因列表");
        }
        return data;
    }

    private StockBasic findStock(String thscode) {
        if (thscode == null || !thscode.matches("[0-9]{6}\\.(SH|SZ|BJ)")) {
            throw new IllegalArgumentException("请输入完整 A 股代码，例如 600519.SH");
        }
        StockBasic stock = stockBasicDao.selectByTsCode(thscode);
        if (stock == null) {
            throw new IllegalArgumentException("未找到该标的，请先在标的数据中同步最新 A 股列表");
        }
        return stock;
    }

    private JSONObject firstQuote(Map<String, Object> response) {
        JSONObject payload = JSONUtil.parseObj(response);
        if (!ApiConstants.STATUS_SUCCESS.equals(payload.getStr(ApiConstants.KEY_STATUS))) {
            throw new IllegalStateException(payload.getStr(ApiConstants.KEY_MESSAGE, "最新行情获取失败"));
        }
        JSONArray rows = payload.getJSONArray(ApiConstants.KEY_DATA);
        if (rows == null || rows.isEmpty()) {
            throw new IllegalStateException("同花顺金融数据 API 未返回最新行情");
        }
        return rows.getJSONObject(0);
    }

    private List<Map<String, Object>> buildBars(Map<String, Object> response) {
        JSONObject payload = JSONUtil.parseObj(response);
        if (!ApiConstants.STATUS_SUCCESS.equals(payload.getStr(ApiConstants.KEY_STATUS))) {
            throw new IllegalStateException(payload.getStr(ApiConstants.KEY_MESSAGE, "历史日K获取失败"));
        }
        JSONObject data = payload.getJSONObject(ApiConstants.KEY_DATA);
        if (data == null) {
            throw new IllegalStateException("历史日K返回格式无效");
        }
        JSONArray timestamps = data.getJSONArray("timestamp");
        JSONArray opens = data.getJSONArray("open");
        JSONArray highs = data.getJSONArray("high");
        JSONArray lows = data.getJSONArray("low");
        JSONArray closes = data.getJSONArray("close");
        JSONArray volumes = data.getJSONArray("volume");
        JSONArray amounts = data.getJSONArray("amount");
        int size = minimumSize(timestamps, opens, highs, lows, closes, volumes, amounts);
        List<Map<String, Object>> bars = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            BigDecimal close = decimal(closes.get(index));
            if (close == null) {
                continue;
            }
            Map<String, Object> bar = new LinkedHashMap<>();
            long timestamp = timestamps.getLong(index);
            bar.put("timestamp", timestamp);
            bar.put("date", Instant.ofEpochMilli(timestamp).atZone(MARKET_ZONE).toLocalDate().format(DATE_FORMATTER));
            bar.put("open", decimal(opens.get(index)));
            bar.put("high", decimal(highs.get(index)));
            bar.put("low", decimal(lows.get(index)));
            bar.put("close", close);
            bar.put("volume", decimal(volumes.get(index)));
            bar.put("amount", decimal(amounts.get(index)));
            bars.add(bar);
        }
        return bars;
    }

    private int minimumSize(JSONArray... arrays) {
        int result = Integer.MAX_VALUE;
        for (JSONArray array : arrays) {
            if (array == null) {
                return 0;
            }
            result = Math.min(result, array.size());
        }
        return result == Integer.MAX_VALUE ? 0 : result;
    }

    private Map<String, Object> quoteData(JSONObject quote) {
        JSONObject ext = quote.getJSONObject("ext");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lastPrice", decimal(quote.get("last_price")));
        result.put("prevClose", decimal(quote.get("prev_close")));
        result.put("open", decimal(quote.get("open")));
        result.put("high", decimal(quote.get("high")));
        result.put("low", decimal(quote.get("low")));
        result.put("volume", decimal(quote.get("volume")));
        result.put("amount", decimal(quote.get("amount")));
        result.put("changeAmount", ext == null ? null : decimal(ext.get("change_amount")));
        result.put("changePct", ext == null ? null : decimal(ext.get("change_pct")));
        result.put("amplitude", ext == null ? null : decimal(ext.get("amplitude")));
        return result;
    }

    private Map<String, Object> calculateMetrics(List<Map<String, Object>> bars) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        BigDecimal firstClose = valueOf(bars.getFirst(), "close");
        BigDecimal latestClose = valueOf(bars.getLast(), "close");
        metrics.put("yearReturn", percentChange(firstClose, latestClose));
        metrics.put("ma5", movingAverage(bars, 5));
        metrics.put("ma20", movingAverage(bars, 20));
        metrics.put("ma30", movingAverage(bars, 30));
        metrics.put("ma60", movingAverage(bars, 60));
        DRAWDOWN_PERIODS.forEach((key, days) -> metrics.put(key, maxDrawdown(bars, days)));
        metrics.put("averageAmount20", averageAmount(bars));
        return metrics;
    }

    private BigDecimal movingAverage(List<Map<String, Object>> bars, int period) {
        if (bars.size() < period) {
            return null;
        }
        return bars.subList(bars.size() - period, bars.size()).stream().map(bar -> valueOf(bar, "close"))
                .reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(period), 4, RoundingMode.HALF_UP);
    }

    /** 按周期内前复权收盘价计算峰值到后续谷值的最大跌幅，历史不足则不提供指标。 */
    private BigDecimal maxDrawdown(List<Map<String, Object>> bars, int days) {
        if (bars.size() < days) {
            return null;
        }
        BigDecimal peak = null;
        BigDecimal worst = BigDecimal.ZERO;
        for (Map<String, Object> bar : bars.subList(bars.size() - days, bars.size())) {
            BigDecimal close = valueOf(bar, "close");
            if (peak == null || close.compareTo(peak) > 0) {
                peak = close;
            }
            if (peak.signum() <= 0) {
                continue;
            }
            BigDecimal drawdown = close.subtract(peak).divide(peak, 8, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            worst = worst.min(drawdown);
        }
        return worst.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageAmount(List<Map<String, Object>> bars) {
        if (bars.size() < AVERAGE_AMOUNT_DAYS) {
            return null;
        }
        List<BigDecimal> amounts = bars.subList(Math.max(0, bars.size() - AVERAGE_AMOUNT_DAYS), bars.size()).stream()
                .map(bar -> valueOf(bar, "amount")).filter(Objects::nonNull).toList();
        if (amounts.isEmpty()) {
            return null;
        }
        return amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentChange(BigDecimal start, BigDecimal end) {
        if (start == null || end == null || start.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return end.subtract(start).divide(start, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal valueOf(Map<String, Object> row, String key) {
        return decimal(row.get(key));
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException error) {
            log.warn("行情字段不是有效数字: {}", value);
            return null;
        }
    }

    private String formatDataTime(long timestamp) {
        if (timestamp <= 0) {
            return null;
        }
        return Instant.ofEpochMilli(timestamp).atZone(MARKET_ZONE).toOffsetDateTime().toString();
    }
}
