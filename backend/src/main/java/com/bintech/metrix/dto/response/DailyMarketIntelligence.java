package com.bintech.metrix.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 首页研究报告；数值均由确定性计算产生，代码由前端国际化渲染。 */
public record DailyMarketIntelligence(
        String asOf, String generatedAt, MarketRegime marketRegime, MarketSnapshot marketSnapshot,
        PortfolioSnapshot portfolioSnapshot, TodayAction todayAction, DataQuality dataQuality,
        List<String> highlights, String narrativeSource) {

    public DailyMarketIntelligence withHighlights(List<String> selected) {
        return new DailyMarketIntelligence(asOf, generatedAt, marketRegime, marketSnapshot, portfolioSnapshot,
                todayAction, dataQuality, selected, "AI_SELECTED_VERIFIED_EVIDENCE");
    }

    public record MarketRegime(String label, String confidence, String riskLevel, String summary,
            List<String> evidence, List<String> counterEvidence, List<String> confirmationSignals) {}

    public record MarketSnapshot(String date, String phase, String scope, String source, String updatedAt,
            Double totalTurnover, Double turnoverChangePct, Double turnoverVs5Pct, Double turnoverVs20Pct,
            Integer upCount, Integer downCount, Integer flatCount, Double upDownRatio, Double upPct, Double downPct,
            Integer breadthNet, Double upPctVs5, Double upPctVs20, Integer limitUp, Integer limitDown,
            Integer streakHeight, Double brokenRate, String marketBreadth, String sentiment, String volumePattern,
            List<IndexView> indices, List<HistoryPoint> history) {}

    public record IndexView(String symbol, String name, String date, Double close, Double changePct,
            Double ma20, Double ma60, Double ma120, Double slope20, Double slope60, Double slope120,
            String highLowStructure, String trend, int historySize, Double completedClose) {}

    public record HistoryPoint(String date, Double turnover, Double upPct, Integer upCount,
            Integer downCount, Integer flatCount, Integer limitDown) {}

    public record HoldingView(String stockCode, String stockName, String industry, BigDecimal quantity,
            BigDecimal totalCost, BigDecimal price, BigDecimal prevClose, BigDecimal marketValue,
            BigDecimal floatingPnl, BigDecimal dailyPnl, BigDecimal weightPct, String quoteStatus) {}

    public record PortfolioSnapshot(int holdingCount, BigDecimal totalCost, BigDecimal marketValue,
            BigDecimal floatingPnl, BigDecimal floatingPnlPct, BigDecimal dailyPnl, BigDecimal dailyReturnPct,
            String benchmark, BigDecimal excessReturnPct, String relativeStrength, String riskExposure,
            BigDecimal stockConcentrationPct, BigDecimal industryConcentrationPct, List<HoldingView> holdings) {}

    public record TodayAction(String label, String reason, List<String> watchItems, List<String> triggerConditions) {}

    public record DataQuality(boolean marketDataComplete, boolean portfolioDataComplete, boolean stale,
            List<String> missingFields, List<String> limitations, Map<String, String> sourceDates) {}

    public record Result(String status, boolean refreshing, DailyMarketIntelligence report, String errorCode) {}
}
