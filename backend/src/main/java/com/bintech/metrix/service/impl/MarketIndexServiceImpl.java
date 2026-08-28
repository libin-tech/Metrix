package com.bintech.metrix.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.CacheConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.config.MarketTurnoverProperties;
import com.bintech.metrix.repository.entity.MarketDataConfig;
import com.bintech.metrix.service.MarketIndexService;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketIndexServiceImpl implements MarketIndexService {

    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${python.akshare-script-path:python-service/akshare.py}")
    private String akshareScriptPath;

    private final RedisCacheService redisCacheService;
    private final MarketTurnoverProperties marketTurnoverProperties;
    private final MarketDataService marketDataService;

    @Override
    public Map<String, Object> getMarketIndex() {
        String scriptPath = akshareScriptPath.replace("akshare.py", "akshare_index_spot.py");

        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(scriptPath);
        command.add("--symbols");
        command.add("sh000001,sz399001,sz399006,sh000688");

        log.info("执行大盘指数实时行情脚本: {}", String.join(" ", command));

        return runScript(command, "MarketIndex", SystemConstants.DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 获取沪深两市合计成交额，并返回最近 60 个交易日趋势。
     *
     * @return 市场成交额数据
     */
    @Override
    public synchronized Map<String, Object> getMarketTurnover(Long userId) {
        Map<String, Object> cachedTurnover = getCachedMarketTurnover(userId);
        if (cachedTurnover != null && isMarketTurnoverFresh(userId)) {
            return cachedTurnover;
        }

        try {
            Map<String, Object> latestTurnover = fetchCurrentMarketTurnover(userId);
            if (!hasValidLatestTurnover(latestTurnover)) {
                throw new RuntimeException("TickFlow市场成交额上游返回数据无效");
            }

            Map<String, Object> turnover = cachedTurnover == null
                    ? mergeLatestTurnover(fetchHistoricalMarketTurnover(), latestTurnover)
                    : mergeLatestTurnover(cachedTurnover, latestTurnover);
            if (!hasValidLatestTurnover(turnover)) {
                throw new RuntimeException("市场成交额刷新结果无效");
            }
            cacheMarketTurnover(userId, turnover);
            return turnover;
        } catch (RuntimeException e) {
            log.warn("市场成交额上游数据暂不可用，返回最近成功结果: {}", e.getMessage());
            return cachedTurnover == null ? createEmptyMarketTurnover() : cachedTurnover;
        }
    }

    private boolean isMarketTurnoverFresh(Long userId) {
        String refreshAt = redisCacheService.get(getMarketTurnoverRefreshAtKey(userId));
        if (refreshAt == null || refreshAt.isBlank()) {
            return false;
        }
        try {
            return System.currentTimeMillis() - Long.parseLong(refreshAt)
                    < SystemConstants.MARKET_TURNOVER_REFRESH_INTERVAL_MILLIS;
        } catch (NumberFormatException e) {
            log.warn("市场成交额缓存刷新时间无效: {}", refreshAt);
            return false;
        }
    }

    /**
     * 通过 TickFlow 获取当前交易日的盘中成交额。
     *
     * @param userId 当前登录用户 ID
     * @return 当前交易日成交额
     */
    private Map<String, Object> fetchCurrentMarketTurnover(Long userId) {
        MarketDataConfig config = marketDataService.getActiveTickFlowConfig(userId);
        String apiKey = config.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("TickFlow市场成交额 API Key 未配置");
        }
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(marketTurnoverProperties.getTickflowKlinesScriptPath());
        command.add("--api-key");
        command.add(apiKey);
        command.add("--symbols");
        command.add(SystemConstants.MARKET_TURNOVER_SHANGHAI_SYMBOL + ","
                + SystemConstants.MARKET_TURNOVER_SHENZHEN_SYMBOL);
        command.add("--period");
        command.add(SystemConstants.KLINE_PERIOD_DAY);
        command.add("--count");
        command.add(String.valueOf(SystemConstants.MARKET_TURNOVER_LATEST_COUNT));
        command.add("--market-turnover");

        log.info("执行TickFlow市场成交额脚本");
        int timeoutSeconds = config.getTimeout() == null
                ? marketTurnoverProperties.getTimeoutSeconds()
                : config.getTimeout();
        return runScript(command, "TickFlowMarketTurnover", timeoutSeconds);
    }

    /**
     * 通过 Baostock 获取最近 60 个交易日的已收盘成交额历史。
     *
     * @return 市场成交额历史数据
     */
    private Map<String, Object> fetchHistoricalMarketTurnover() {
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(marketTurnoverProperties.getBaostockMarketTurnoverScriptPath());
        command.add("--count");
        command.add(String.valueOf(SystemConstants.MARKET_TURNOVER_HISTORY_SIZE));

        log.info("执行Baostock市场成交额历史脚本");
        return runScript(command, "BaostockMarketTurnover", marketTurnoverProperties.getTimeoutSeconds());
    }

    private static Map<String, Object> mergeLatestTurnover(Map<String, Object> cachedTurnover,
                                                            Map<String, Object> latestTurnover) {
        JSONObject cachedResult = JSONUtil.parseObj(cachedTurnover);
        if (!hasValidLatestTurnover(latestTurnover)) {
            return cachedResult;
        }
        JSONObject cachedData = cachedResult.getJSONObject(ApiConstants.KEY_DATA);
        JSONObject latestData = JSONUtil.parseObj(latestTurnover).getJSONObject(ApiConstants.KEY_DATA);
        JSONArray history = cachedData == null ? null : cachedData.getJSONArray("history");
        JSONArray latestHistory = latestData == null ? null : latestData.getJSONArray("history");

        if (history == null || history.isEmpty() || latestHistory == null || latestHistory.isEmpty()) {
            throw new RuntimeException("市场成交额缓存或最新数据为空");
        }

        JSONObject latestItem = latestHistory.getJSONObject(latestHistory.size() - 1);
        String latestDate = latestItem.getStr("date");
        if (!isToday(latestDate)) {
            return cachedResult;
        }
        boolean updated = false;
        for (int index = history.size() - 1; index >= 0; index--) {
            if (latestDate.equals(history.getJSONObject(index).getStr("date"))) {
                history.set(index, latestItem);
                updated = true;
                break;
            }
        }
        if (!updated) history.add(latestItem);
        while (history.size() > SystemConstants.MARKET_TURNOVER_HISTORY_SIZE) history.remove(0);

        JSONObject current = history.getJSONObject(history.size() - 1);
        JSONObject previous = history.size() > 1 ? history.getJSONObject(history.size() - 2) : current;
        cachedData.put("amount", current.getLong("amount", 0L));
        cachedData.put("difference", current.getLong("amount", 0L) - previous.getLong("amount", 0L));
        cachedData.put("history", history);
        cachedResult.put(ApiConstants.KEY_DATA, cachedData);
        return cachedResult;
    }

    private static boolean hasValidLatestTurnover(Map<String, Object> turnover) {
        if (turnover == null || turnover.isEmpty()) {
            return false;
        }
        JSONObject result = JSONUtil.parseObj(turnover);
        JSONObject data = result.getJSONObject(ApiConstants.KEY_DATA);
        JSONArray history = data == null ? null : data.getJSONArray("history");
        if (!ApiConstants.STATUS_SUCCESS.equals(result.getStr(ApiConstants.KEY_STATUS))
                || history == null || history.isEmpty()) {
            return false;
        }

        JSONObject latestItem = history.getJSONObject(history.size() - 1);
        if (latestItem == null) {
            return false;
        }
        String latestDate = latestItem.getStr("date");
        Long latestAmount = latestItem.getLong("amount");
        return latestDate != null && !latestDate.isBlank() && latestAmount != null && latestAmount >= 0;
    }

    /**
     * 仅允许刷新中国时区的当天成交额，避免非交易日或上游延迟数据覆写历史缓存。
     *
     * @param tradeDate 上游返回的交易日期
     * @return 是否为当天
     */
    private static boolean isToday(String tradeDate) {
        return LocalDate.now(ZoneId.of(SystemConstants.MARKET_TIME_ZONE_ID)).toString().equals(tradeDate);
    }

    @Override
    public Map<String, Object> getCachedMarketTurnover(Long userId) {
        String cacheValue = redisCacheService.get(getMarketTurnoverLastSuccessKey(userId));
        if (cacheValue == null || cacheValue.isBlank()) {
            cacheValue = redisCacheService.get(CacheConstants.MARKET_TURNOVER_LEGACY_LAST_SUCCESS_KEY);
        }
        if (cacheValue == null || cacheValue.isBlank()) {
            return null;
        }
        Map<String, Object> cachedTurnover = new HashMap<>(JSONUtil.parseObj(cacheValue));
        return hasValidLatestTurnover(cachedTurnover) ? cachedTurnover : null;
    }

    private void cacheMarketTurnover(Long userId, Map<String, Object> marketTurnover) {
        redisCacheService.setJson(getMarketTurnoverLastSuccessKey(userId), marketTurnover);
        redisCacheService.set(getMarketTurnoverRefreshAtKey(userId),
                String.valueOf(System.currentTimeMillis()));
    }

    private String getMarketTurnoverLastSuccessKey(Long userId) {
        return CacheConstants.MARKET_TURNOVER_LAST_SUCCESS_KEY_PREFIX + userId;
    }

    private String getMarketTurnoverRefreshAtKey(Long userId) {
        return CacheConstants.MARKET_TURNOVER_REFRESH_AT_KEY_PREFIX + userId;
    }

    private Map<String, Object> createEmptyMarketTurnover() {
        Map<String, Object> data = new HashMap<>();
        data.put("history", Collections.emptyList());

        Map<String, Object> result = new HashMap<>();
        result.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_SUCCESS);
        result.put(ApiConstants.KEY_DATA, data);
        return result;
    }

    private Map<String, Object> runScript(List<String> command, String sourceName, int timeoutSeconds) {
        String timeoutMsg = sourceName + "脚本执行超时（" + timeoutSeconds + "秒）";

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            Process process = pb.start();

            StringBuilder outputBuilder = new StringBuilder();
            Thread reader = Thread.ofVirtual()
                    .name("market-index-reader")
                    .start(() -> {
                        try (BufferedReader br = new BufferedReader(
                                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                outputBuilder.append(line).append('\n');
                            }
                        } catch (IOException e) {
                            log.warn("读取{}脚本输出流异常: {}", sourceName, e.getMessage());
                        }
                    });

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            reader.join(SystemConstants.READER_JOIN_TIMEOUT_MILLIS);

            if (!finished) {
                process.destroyForcibly();
                log.error("{}", timeoutMsg);
                throw new RuntimeException(timeoutMsg);
            }

            String output = outputBuilder.toString().trim();
            if (output.isEmpty()) {
                log.error("{}脚本输出为空", sourceName);
                throw new RuntimeException(sourceName + "数据获取失败: 脚本输出为空");
            }
            log.info("{}脚本输出: {}", sourceName, output);

            JSONObject json = JSONUtil.parseObj(output);

            if (!ApiConstants.STATUS_SUCCESS.equals(json.getStr(ApiConstants.KEY_STATUS))) {
                String msg = json.getStr(ApiConstants.KEY_MESSAGE, sourceName + "脚本执行失败");
                log.error("{}脚本返回错误: {}", sourceName, msg);
                throw new RuntimeException(sourceName + "数据获取失败: " + msg);
            }

            log.debug("{}脚本返回结果: {}", sourceName, json);
            return json;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("执行{}脚本异常: {}", sourceName, e.getMessage(), e);
            throw new RuntimeException(sourceName + "数据获取异常: " + e.getMessage());
        }
    }
}
