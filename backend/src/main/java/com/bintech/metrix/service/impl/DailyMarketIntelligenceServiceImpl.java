package com.bintech.metrix.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.core.analysis.DailyMarketAnalyzer;
import com.bintech.metrix.core.analysis.MarketReviewDataFetcher;
import com.bintech.metrix.core.analysis.MarketSnapshotCalculator;
import com.bintech.metrix.core.analysis.PortfolioSnapshotCalculator;
import com.bintech.metrix.dto.response.DailyMarketIntelligence;
import com.bintech.metrix.dto.response.DailyMarketIntelligence.*;
import com.bintech.metrix.repository.dao.PortfolioHoldingDao;
import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.entity.PortfolioHolding;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.DailyMarketIntelligenceService;
import com.bintech.metrix.service.MarketActivityService;
import com.bintech.metrix.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.bintech.metrix.constants.CacheConstants.*;
import static com.bintech.metrix.constants.MarketIntelligenceConstants.*;

/** 共享市场快照，按登录用户隔离报告；请求线程不等待行情或模型调用。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailyMarketIntelligenceServiceImpl implements DailyMarketIntelligenceService {
    private final MarketActivityService activityService;
    private final RedisCacheService cache;
    private final MarketReviewDataFetcher indexFetcher;
    private final PortfolioHoldingDao holdingsDao;
    private final StockBasicDao stocksDao;
    private final MarketSnapshotCalculator marketCalculator;
    private final PortfolioSnapshotCalculator portfolioCalculator;
    private final DailyMarketAnalyzer analyzer;
    private final AiModelService aiService;
    private final Map<Long, ReportEntry> reports = new ConcurrentHashMap<>();
    private SharedMarket shared;
    private Instant marketAttempt = Instant.EPOCH;

    private static final class ReportEntry {
        private DailyMarketIntelligence report;
        private boolean refreshing;
        private Instant attempted = Instant.EPOCH;
        private String error;
    }
    private record SharedMarket(JSONObject source, MarketSnapshot market) {}

    @Override
    public Result getReport(Long userId) {
        if (userId == null) throw new IllegalArgumentException("Login required");
        evictOldReports();
        ReportEntry entry = reports.computeIfAbsent(userId, ignored -> new ReportEntry());
        synchronized (entry) {
            boolean due = durationSince(entry.attempted) >= (entry.error == null ? REFRESH_INTERVAL : RETRY_INTERVAL).toMillis();
            if (!entry.refreshing && due) {
                entry.refreshing = true;
                entry.attempted = Instant.now();
                Thread.startVirtualThread(() -> refresh(userId, entry));
            }
            DailyMarketIntelligence current = analyzer.refreshQuality(entry.report, OffsetDateTime.now(ZONE), entry.error != null);
            String status = current == null ? (entry.error == null ? "LOADING" : "ERROR") : (current.dataQuality().stale() ? "STALE" : "READY");
            return new Result(status, entry.refreshing, current, entry.error);
        }
    }

    @Override
    public void refreshMarketHistory() {
        try {
            market();
        } catch (RuntimeException error) {
            log.warn("收盘研究快照归档失败: {}", error.getMessage());
        }
    }

    private long durationSince(Instant time) {
        return Instant.now().toEpochMilli() - time.toEpochMilli();
    }

    private void evictOldReports() {
        if (reports.size() < CACHE_LIMIT) return;
        reports.entrySet().removeIf(item -> removable(item.getValue()));
    }

    private boolean removable(ReportEntry entry) {
        synchronized (entry) {
            return !entry.refreshing && durationSince(entry.attempted) > STALE_AFTER.toMillis();
        }
    }

    private void refresh(Long userId, ReportEntry entry) {
        try {
            SharedMarket data = market();
            List<PortfolioHolding> holdings = holdingsDao.selectByUserId(userId);
            List<String> codes = holdings.stream().map(PortfolioHolding::getStockCode).distinct().toList();
            Map<String, String> industries = new HashMap<>();
            List<StockBasic> stocks = codes.isEmpty() ? List.of() : stocksDao.selectByTsCodeIn(codes);
            stocks.stream().filter(stock -> stock.getIndustry() != null).forEach(stock -> industries.put(stock.getTsCode(), stock.getIndustry()));
            PortfolioSnapshot portfolio = portfolioCalculator.calculate(holdings, industries, data.source().getJSONObject("quotes"), data.market());
            DailyMarketIntelligence report = analyzer.analyze(data.market(), portfolio, data.source(), OffsetDateTime.now(ZONE));
            synchronized (entry) {
                entry.report = report;
                entry.error = null;
            }
            selectHighlights(userId, entry, report);
        } catch (RuntimeException error) {
            log.warn("每日市场提示生成失败 userId={}: {}", userId, error.getMessage());
            synchronized (entry) { entry.error = "DATA_UNAVAILABLE"; }
        } finally {
            synchronized (entry) { entry.refreshing = false; }
        }
    }

    /** 归档同口径收盘广度与成交额，不拿既有沪深历史冒充沪深京历史。 */
    private synchronized SharedMarket market() {
        if (shared != null && durationSince(marketAttempt) < REFRESH_INTERVAL.toMillis()) return shared;
        marketAttempt = Instant.now();
        JSONObject response = JSONUtil.parseObj(activityService.getMarketActivity());
        JSONObject publicData = response.getJSONObject("data");
        String raw = cache.get(MARKET_RESEARCH_QUOTES_KEY);
        if (raw == null) throw new IllegalStateException("Research snapshot unavailable");
        JSONObject source = JSONUtil.parseObj(raw);
        source.set("stale", publicData == null || publicData.getBool("stale", false));
        String date = MarketSnapshotCalculator.date(source.getStr("statDate"));
        if (date == null || !SCOPE.equals(source.getStr("scope"))) throw new IllegalStateException("Research snapshot scope/date invalid");
        List<JSONObject> history = archive(source);
        Map<String, Object> indices = fetchIndices(date);
        shared = new SharedMarket(source, marketCalculator.calculate(source, history, indices));
        return shared;
    }

    /** 指数源故障不丢弃已获取的市场和持仓数据；缺失由质量层明确展示。 */
    private Map<String, Object> fetchIndices(String date) {
        try {
            return indexFetcher.fetchIndexData(date, INDICES, INDEX_HISTORY_COUNT);
        } catch (RuntimeException error) {
            log.warn("研究指数数据未获取，按缺失指标展示: {}", error.getMessage());
            return Map.of();
        }
    }

    private List<JSONObject> archive(JSONObject source) {
        String raw = cache.get(MARKET_RESEARCH_HISTORY_KEY);
        List<JSONObject> rows = raw == null ? List.of() : JSONUtil.parseArray(raw).toList(JSONObject.class);
        TreeMap<String, JSONObject> dates = new TreeMap<>();
        rows.stream().filter(row -> SCOPE.equals(row.getStr("scope")) && MarketSnapshotCalculator.date(row.getStr("statDate")) != null)
                .forEach(row -> dates.put(MarketSnapshotCalculator.date(row.getStr("statDate")), row));
        if ("CLOSED".equals(source.getStr("sessionPhase")) && !source.getBool("stale", false)) {
            JSONObject closed = JSONUtil.parseObj(source.toString());
            closed.remove("quotes");
            dates.put(MarketSnapshotCalculator.date(source.getStr("statDate")), closed);
        }
        while (dates.size() > HISTORY_LIMIT) dates.pollFirstEntry();
        List<JSONObject> history = new ArrayList<>(dates.values());
        cache.setJson(MARKET_RESEARCH_HISTORY_KEY, history);
        return history;
    }

    private void selectHighlights(Long userId, ReportEntry entry, DailyMarketIntelligence report) {
        try {
            if (!aiService.hasActiveConfig(userId)) return;
            List<String> allowed = report.marketRegime().evidence();
            String answer = modelAnswer(userId, allowed);
            List<String> selected = validateHighlights(answer, allowed);
            if (selected.isEmpty()) return;
            synchronized (entry) { entry.report = report.withHighlights(selected); }
        } catch (RuntimeException error) {
            log.debug("AI 摘要排序不可用，保留确定性证据: {}", error.getMessage());
        }
    }

    private String modelAnswer(Long userId, List<String> allowed) {
        FutureTask<String> request = new FutureTask<>(() -> aiService.generateAnalysis(
                AI_HIGHLIGHT_PROMPT + JSONUtil.toJsonStr(allowed), aiService.getActiveModelType(userId), userId));
        Thread.startVirtualThread(request);
        try {
            return request.get(AI_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Evidence ranking interrupted", error);
        } catch (ExecutionException | TimeoutException error) {
            throw new IllegalStateException("Evidence ranking unavailable", error);
        } finally {
            request.cancel(true);
        }
    }

    /** 模型只能挑选存在的证据，任何新增事实、重复或错误格式均拒绝。 */
    public static List<String> validateHighlights(String answer, List<String> allowed) {
        if (answer == null || !JSONUtil.isTypeJSONArray(answer)) return List.of();
        List<String> selected = JSONUtil.parseArray(answer).toList(String.class);
        boolean valid = selected.size() >= MIN_HIGHLIGHTS && selected.size() <= MAX_HIGHLIGHTS
                && selected.stream().distinct().count() == selected.size() && allowed.containsAll(selected);
        return valid ? selected : List.of();
    }
}
