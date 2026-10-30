package com.bintech.metrix.core.analysis;

import cn.hutool.json.JSONObject;
import com.bintech.metrix.dto.response.DailyMarketIntelligence;
import com.bintech.metrix.dto.response.DailyMarketIntelligence.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.bintech.metrix.constants.MarketIntelligenceConstants.*;
import static com.bintech.metrix.core.analysis.PortfolioSnapshotCalculator.above;

/** 独立证据分组决策；相同广度信号不会以“情绪”名义重复计票。 */
@Component
public class DailyMarketAnalyzer {
    /** 缺失数据降低判断强度；成本和浮亏不作为退出指令。 */
    public DailyMarketIntelligence analyze(MarketSnapshot market, PortfolioSnapshot portfolio, JSONObject source, OffsetDateTime now) {
        List<String> missing = missingFields(market, portfolio);
        boolean stale = source.getBool("stale", false) || outdated(market.updatedAt(), now);
        boolean aligned = market.indices().stream().allMatch(index -> market.date().equals(index.date()) && index.changePct() != null);
        String cycle = cycle(market, aligned);
        boolean trendRisk = "BEAR".equals(cycle) || "BEAR_RISK".equals(cycle);
        boolean breadthRisk = "WEAK".equals(market.marketBreadth());
        boolean volumeRisk = "DOWN_EXPANDING".equals(market.volumePattern()) || persistentContraction(market);
        boolean portfolioRisk = above(portfolio.stockConcentrationPct(), STOCK_CONCENTRATION)
                || above(portfolio.industryConcentrationPct(), INDUSTRY_CONCENTRATION)
                || "UNDERPERFORM".equals(portfolio.relativeStrength());
        int groups = (trendRisk ? 1 : 0) + (breadthRisk ? 1 : 0) + (volumeRisk ? 1 : 0) + (portfolioRisk ? 1 : 0);
        boolean essentialMissing = stale || market.upPct() == null || market.totalTurnover() == null || !aligned || portfolio.marketValue() == null;
        String risk = essentialMissing ? "UNKNOWN" : MarketResearchRules.classify(groups, List.of(
                new MarketResearchRules.Rule<>("HIGH", count -> count >= 3 && breadthRisk),
                new MarketResearchRules.Rule<>("ELEVATED", count -> count >= 2),
                new MarketResearchRules.Rule<>("LOW", count -> count == 0 && "BULL".equals(cycle) && "STRONG".equals(market.marketBreadth())
                        && !"OVERHEATED".equals(market.sentiment()) && !"UNKNOWN".equals(portfolio.riskExposure()))), "MEDIUM");
        List<String> evidence = evidence(market, portfolio, cycle);
        List<String> counter = counterEvidence(market, cycle);
        List<String> confirmations = List.of("CONFIRM_TREND", "CONFIRM_BREADTH_VOLUME", "CONFIRM_PORTFOLIO");
        String action = MarketResearchRules.classify(risk, List.of(
                new MarketResearchRules.Rule<>("WAIT_CONFIRMATION", value -> "UNKNOWN".equals(value)),
                new MarketResearchRules.Rule<>("REDUCE_IF_CONFIRMED", "HIGH"::equals),
                new MarketResearchRules.Rule<>("REVIEW_EXPOSURE", value -> portfolioRisk),
                new MarketResearchRules.Rule<>("PAUSE_ADDING", "ELEVATED"::equals),
                new MarketResearchRules.Rule<>("WAIT_CONFIRMATION", value -> "INSUFFICIENT".equals(cycle)),
                new MarketResearchRules.Rule<>("OBSERVE", "LOW"::equals)), "CAUTIOUS_HOLD");
        List<String> limitations = new ArrayList<>(List.of("SNAPSHOT_NOT_ACCOUNT_RETURN", "SOURCE_TIME_INFERRED", "INDEX_TIME_SEPARATE",
                "LIMIT_SCOPE_SH_SZ", "NO_FUNDAMENTAL_CAUSALITY", "NO_RISK_REWARD", "NO_MACRO_EVIDENCE", "TURNOVER_NOT_NET_INFLOW"));
        if ("INTRADAY".equals(market.phase())) limitations.add("INTRADAY_NO_DAILY_COMPARISON");
        if (stale) limitations.add("STALE_DATA");
        if (!aligned) limitations.add("DATES_MISMATCH");
        Map<String, String> dates = new LinkedHashMap<>();
        dates.put("market", market.date());
        dates.put("portfolioQuotes", market.date());
        market.indices().forEach(index -> dates.put(index.symbol(), index.date() == null ? "UNKNOWN" : index.date()));
        boolean marketComplete = missing.stream().noneMatch(field -> !field.startsWith("portfolio"));
        boolean portfolioComplete = missing.stream().noneMatch(field -> field.startsWith("portfolio"));
        String confidence = essentialMissing || "INSUFFICIENT".equals(cycle) || !marketComplete ? "LOW" : "MEDIUM";
        List<String> highlights = evidence.stream().limit(MAX_HIGHLIGHTS).toList();
        return new DailyMarketIntelligence(market.updatedAt(), now.toString(),
                new MarketRegime(cycle, confidence, risk, "SUMMARY_" + market.marketBreadth(), evidence, counter, confirmations),
                market, portfolio, new TodayAction(action, "REASON_" + action, evidence, confirmations),
                new DataQuality(marketComplete, portfolioComplete, stale, missing, limitations, dates), highlights, "VERIFIED_RULES");
    }

    /** 缓存读取也重新检查时效，刷新期间不能把旧报告继续当作当前判断。 */
    public DailyMarketIntelligence refreshQuality(DailyMarketIntelligence report, OffsetDateTime now, boolean failed) {
        if (report == null || !failed && !outdated(report.asOf(), now) && !report.dataQuality().stale()) return report;
        MarketRegime regime = report.marketRegime();
        DataQuality quality = report.dataQuality();
        List<String> limitations = new ArrayList<>(quality.limitations());
        if (!limitations.contains("STALE_DATA")) limitations.add("STALE_DATA");
        return new DailyMarketIntelligence(report.asOf(), report.generatedAt(),
                new MarketRegime(regime.label(), "LOW", "UNKNOWN", regime.summary(), regime.evidence(), regime.counterEvidence(), regime.confirmationSignals()),
                report.marketSnapshot(), report.portfolioSnapshot(),
                new TodayAction("WAIT_CONFIRMATION", "REASON_WAIT_CONFIRMATION", report.todayAction().watchItems(), report.todayAction().triggerConditions()),
                new DataQuality(quality.marketDataComplete(), quality.portfolioDataComplete(), true, quality.missingFields(), limitations, quality.sourceDates()),
                report.highlights(), report.narrativeSource());
    }

    private List<String> missingFields(MarketSnapshot market, PortfolioSnapshot portfolio) {
        Map<String, Boolean> checks = new LinkedHashMap<>();
        checks.put("turnover", market.totalTurnover() == null);
        checks.put("turnoverHistory", market.turnoverVs20Pct() == null && "CLOSED".equals(market.phase()));
        checks.put("breadth", market.upPct() == null);
        checks.put("breadthHistory", market.upPctVs20() == null);
        checks.put("indexHistory", market.indices().stream().anyMatch(index -> index.slope120() == null));
        checks.put("indexQuotes", market.indices().stream().anyMatch(index -> !market.date().equals(index.date()) || index.changePct() == null));
        checks.put("limitPools", market.limitDown() == null || market.limitUp() == null);
        checks.put("streakHeight", market.streakHeight() == null);
        checks.put("brokenRate", market.brokenRate() == null);
        checks.put("portfolioQuotes", portfolio.marketValue() == null || portfolio.dailyPnl() == null);
        checks.put("portfolioCost", portfolio.totalCost() == null || portfolio.holdingCount() > 0 && portfolio.floatingPnlPct() == null);
        checks.put("portfolioIndustry", portfolio.holdingCount() > 0 && portfolio.industryConcentrationPct() == null);
        checks.put("portfolioBenchmark", portfolio.holdingCount() > 0 && portfolio.excessReturnPct() == null);
        return checks.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
    }

    private List<String> evidence(MarketSnapshot market, PortfolioSnapshot portfolio, String cycle) {
        List<String> result = new ArrayList<>();
        result.add("BREADTH_" + market.marketBreadth());
        result.add("CYCLE_" + cycle);
        result.add("PORTFOLIO_" + portfolio.riskExposure());
        result.add("RELATIVE_" + portfolio.relativeStrength());
        result.add("VOLUME_" + market.volumePattern());
        Double benchmark = market.indices().stream().filter(index -> "sh000300".equals(index.symbol()) && market.date().equals(index.date()))
                .map(IndexView::changePct).filter(value -> value != null).findFirst().orElse(null);
        if (benchmark != null && benchmark > 0 && "WEAK".equals(market.marketBreadth())) result.addFirst("INDEX_UP_BREADTH_WEAK");
        if (benchmark != null && benchmark < 0 && "STRONG".equals(market.marketBreadth())) result.addFirst("INDEX_DOWN_BREADTH_STRONG");
        if (persistentContraction(market)) result.add("PERSISTENT_CONTRACTION");
        if (spreadingDeclines(market)) result.addFirst("SPREADING_DECLINES");
        if (market.upPctVs5() != null && market.upPctVs20() != null && market.upPctVs5() > 0 && market.upPctVs20() > 0) result.add("BREADTH_RECOVERY");
        return List.copyOf(result);
    }

    private List<String> counterEvidence(MarketSnapshot market, String cycle) {
        List<String> result = new ArrayList<>();
        if (("BEAR".equals(cycle) || "BEAR_RISK".equals(cycle)) && "STRONG".equals(market.marketBreadth())) result.add("BREADTH_RECOVERY");
        if (cycle.startsWith("BULL") && "WEAK".equals(market.marketBreadth())) result.add("BREADTH_WEAK");
        if (market.indices().stream().map(IndexView::trend).distinct().count() > 1) result.add("INDEX_DIVERGENCE");
        if ("INSUFFICIENT".equals(cycle)) result.add("HISTORY_INSUFFICIENT");
        if (result.isEmpty()) result.add("NO_CONFIRMED_COUNTER_EVIDENCE");
        return result;
    }

    private String cycle(MarketSnapshot market, boolean aligned) {
        if (market.upPctVs20() == null || !aligned) return "INSUFFICIENT";
        String technical = MarketResearchRules.cycle(market.indices());
        boolean weakening = market.upPctVs20() < 0 || persistentContraction(market);
        return "BULL".equals(technical) && weakening ? "BULL_DIVERGENCE" : technical;
    }

    private boolean persistentContraction(MarketSnapshot market) {
        if (market.turnoverVs5Pct() == null || market.turnoverVs20Pct() == null
                || market.turnoverVs5Pct() > -VOLUME_CHANGE || market.turnoverVs20Pct() > -VOLUME_CHANGE
                || market.history().size() < MEDIUM_PERIOD) return false;
        List<HistoryPoint> recent = market.history().subList(market.history().size() - SHORT_PERIOD, market.history().size());
        if (recent.stream().anyMatch(point -> point.turnover() == null)) return false;
        return IntStream.range(1, recent.size()).allMatch(index -> recent.get(index).turnover() < recent.get(index - 1).turnover());
    }

    private boolean spreadingDeclines(MarketSnapshot market) {
        if (!"CLOSED".equals(market.phase()) || market.upPctVs5() == null || market.history().size() < SHORT_PERIOD) return false;
        List<HistoryPoint> recent = market.history().subList(market.history().size() - SHORT_PERIOD, market.history().size());
        if (recent.stream().anyMatch(point -> point.downCount() == null || point.limitDown() == null)) return false;
        return recent.getLast().downCount() > recent.getFirst().downCount() && recent.getLast().limitDown() > recent.getFirst().limitDown()
                && market.upPctVs5() < 0;
    }

    private boolean outdated(String timestamp, OffsetDateTime now) {
        try {
            Duration age = Duration.between(OffsetDateTime.parse(timestamp), now);
            return age.isNegative() || age.compareTo(STALE_AFTER) > 0;
        } catch (DateTimeParseException | NullPointerException error) {
            return true;
        }
    }
}
