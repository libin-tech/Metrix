package com.bintech.metrix.core.analysis;

import cn.hutool.json.JSONObject;
import com.bintech.metrix.dto.response.DailyMarketIntelligence.*;
import com.bintech.metrix.repository.entity.PortfolioHolding;
import com.bintech.metrix.service.impl.DailyMarketIntelligenceServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static com.bintech.metrix.constants.MarketIntelligenceConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class DailyMarketIntelligenceTest {
    private final MarketSnapshotCalculator marketCalculator = new MarketSnapshotCalculator();
    private final PortfolioSnapshotCalculator portfolioCalculator = new PortfolioSnapshotCalculator();
    private final DailyMarketAnalyzer analyzer = new DailyMarketAnalyzer();
    private final LocalDate date = LocalDate.of(2026, 9, 15);
    private final OffsetDateTime now = OffsetDateTime.parse("2026-09-15T16:00:00+08:00");

    private JSONObject snapshot(boolean closed) {
        return new JSONObject().set("statDate", date.toString()).set("previousTradeDate", date.minusDays(1).toString())
                .set("sessionPhase", closed ? "CLOSED" : "INTRADAY").set("scope", SCOPE).set("fetchedAt", now.toString())
                .set("totalTurnover", 1200).set("up", 60).set("down", 30).set("flat", 10).set("limitUp", 20).set("limitDown", 2);
    }

    private List<JSONObject> history() {
        List<JSONObject> history = new ArrayList<>();
        for (int i = 20; i > 0; i--) history.add(new JSONObject().set("statDate", date.minusDays(i).toString())
                .set("previousTradeDate", date.minusDays(i + 1).toString()).set("scope", SCOPE)
                .set("sessionPhase", "CLOSED").set("totalTurnover", 1000).set("up", 40).set("down", 50).set("flat", 10));
        return history;
    }

    private Map<String, Object> indices(int count, boolean falling) {
        Map<String, Object> data = new HashMap<>();
        for (String name : INDICES.keySet()) {
            List<JSONObject> records = new ArrayList<>();
            for (int i = count - 1; i >= 0; i--) {
                double value = falling ? 1000 + i : 1000 - i;
                records.add(new JSONObject().set("date", date.minusDays(i).toString()).set("close", value)
                        .set("high", value + 1).set("low", value - 1));
            }
            data.put(name, new JSONObject().set("records", records));
        }
        return data;
    }

    private PortfolioHolding holding(String code, String cost, String quantity) {
        PortfolioHolding item = new PortfolioHolding();
        item.setStockCode(code);
        item.setCost(new BigDecimal(cost));
        item.setQuantity(new BigDecimal(quantity));
        return item;
    }

    private JSONObject quote(String code, double price, double previous) {
        return new JSONObject().set(code, new JSONObject().set("price", price).set("prevClose", previous));
    }

    @Test void computesTurnoverBreadthAndContinuousBenchmarks() {
        var market = marketCalculator.calculate(snapshot(true), history(), indices(130, false));
        assertEquals(20.0, market.turnoverChangePct());
        assertEquals(20.0, market.turnoverVs5Pct());
        assertEquals(20.0, market.turnoverVs20Pct());
        assertEquals(60.0, market.upPct());
        assertEquals(30.0, market.downPct());
        assertEquals(2.0, market.upDownRatio());
        assertEquals(30, market.breadthNet());
        assertEquals(20.0, market.upPctVs20());
        assertEquals("UP_EXPANDING", market.volumePattern());
    }

    @Test void intradayDoesNotCompareTurnoverOrUseCurrentCloseForCycle() {
        var raw = indices(130, false);
        raw.values().forEach(value -> ((JSONObject) value).getJSONArray("records").getJSONObject(129).set("close", 1));
        var market = marketCalculator.calculate(snapshot(false), history(), raw);
        assertNull(market.turnoverChangePct());
        assertNull(market.turnoverVs5Pct());
        assertNull(market.turnoverVs20Pct());
        assertEquals("BULL", MarketResearchRules.cycle(market.indices()));
        assertEquals(129, market.indices().getFirst().historySize());
    }

    @Test void missingOrWrongScopeHistoryIsNotAZeroOrTwentyDayBaseline() {
        var rows = history();
        rows.get(10).set("scope", "SH_SZ_A");
        var market = marketCalculator.calculate(snapshot(true), rows, indices(30, false));
        assertNull(market.turnoverVs20Pct());
        assertNotNull(market.turnoverVs5Pct());
        assertEquals("INSUFFICIENT", MarketResearchRules.cycle(market.indices()));
        rows.removeLast();
        market = marketCalculator.calculate(snapshot(true), rows, Map.of());
        assertNull(market.turnoverChangePct());
        assertNull(market.turnoverVs5Pct());
    }

    @Test void invalidNumbersAndZeroDenominatorsRemainMissing() {
        var raw = snapshot(true).set("totalTurnover", "NaN").set("down", 0);
        var market = marketCalculator.calculate(raw, List.of(), Map.of());
        assertNull(market.totalTurnover());
        assertNull(market.upDownRatio());
        assertNull(IntelligenceNumbers.number("Infinity"));
        assertNull(IntelligenceNumbers.percent(BigDecimal.ONE, BigDecimal.ZERO));
    }

    @Test void computesActualQuantityPnlAndMergesStocksAcrossAccountsForConcentration() {
        var market = marketCalculator.calculate(snapshot(true), history(), indices(130, false));
        var positions = List.of(holding("600001.SH", "8", "100"), holding("600001.SH", "10", "100"));
        var portfolio = portfolioCalculator.calculate(positions, Map.of("600001.SH", "银行"), quote("600001.SH", 12, 10), market);
        assertEquals(0, portfolio.marketValue().compareTo(new BigDecimal("2400")));
        assertEquals(0, portfolio.totalCost().compareTo(new BigDecimal("1800")));
        assertEquals(0, portfolio.floatingPnl().compareTo(new BigDecimal("600")));
        assertEquals(0, portfolio.dailyPnl().compareTo(new BigDecimal("400")));
        assertEquals(new BigDecimal("33.33"), portfolio.floatingPnlPct());
        assertEquals(new BigDecimal("20.00"), portfolio.dailyReturnPct());
        assertEquals(new BigDecimal("100.00"), portfolio.stockConcentrationPct());
        assertEquals(new BigDecimal("100.00"), portfolio.industryConcentrationPct());
        assertEquals("CONCENTRATED", portfolio.riskExposure());
    }

    @Test void missingQuoteDoesNotReportPartialPortfolioAsComplete() {
        var portfolio = portfolioCalculator.calculate(List.of(holding("600001.SH", "8", "100"), holding("000001.SZ", "10", "100")),
                Map.of(), quote("600001.SH", 12, 10), marketCalculator.calculate(snapshot(true), List.of(), Map.of()));
        assertNull(portfolio.marketValue());
        assertNull(portfolio.dailyPnl());
        assertNull(portfolio.stockConcentrationPct());
        assertNull(portfolio.industryConcentrationPct());
        assertEquals("UNKNOWN", portfolio.riskExposure());
    }

    @Test void zeroCostAndMissingIndustryDoNotInventRatios() {
        var portfolio = portfolioCalculator.calculate(List.of(holding("600001.SH", "0", "100")), Map.of(),
                quote("600001.SH", 12, 10), marketCalculator.calculate(snapshot(true), List.of(), Map.of()));
        assertNull(portfolio.floatingPnlPct());
        assertNull(portfolio.excessReturnPct());
        assertNull(portfolio.industryConcentrationPct());
        assertEquals(new BigDecimal("100.00"), portfolio.stockConcentrationPct());
    }

    @Test void emptyPortfolioHasNoInventedReturnOrRiskReward() {
        var portfolio = portfolioCalculator.calculate(List.of(), Map.of(), new JSONObject(), marketCalculator.calculate(snapshot(true), List.of(), Map.of()));
        assertEquals(BigDecimal.ZERO, portfolio.marketValue());
        assertNull(portfolio.dailyReturnPct());
        assertEquals("EMPTY", portfolio.riskExposure());
    }

    @Test void singleWeakDayCannotByItselfDeclareBearCycleOrHighRisk() {
        var source = snapshot(true).set("up", 20).set("down", 70);
        var market = marketCalculator.calculate(source, List.of(), indices(130, false));
        var portfolio = portfolioCalculator.calculate(List.of(), Map.of(), new JSONObject(), market);
        var report = analyzer.analyze(market, portfolio, source, now);
        assertEquals("INSUFFICIENT", report.marketRegime().label());
        assertEquals("MEDIUM", report.marketRegime().riskLevel());
        assertEquals("WAIT_CONFIRMATION", report.todayAction().label());
        assertFalse(report.dataQuality().marketDataComplete());
        assertEquals("LOW", report.marketRegime().confidence());
    }

    @Test void multipleIndependentDeterioratingGroupsRaiseRiskWithConditionalAction() {
        var source = snapshot(true).set("up", 10).set("down", 80);
        var market = marketCalculator.calculate(source, history(), indices(130, true));
        var portfolio = portfolioCalculator.calculate(List.of(), Map.of(), new JSONObject(), market);
        var report = analyzer.analyze(market, portfolio, source, now);
        assertEquals("BEAR", report.marketRegime().label());
        assertEquals("HIGH", report.marketRegime().riskLevel());
        assertEquals("REDUCE_IF_CONFIRMED", report.todayAction().label());
        assertFalse(report.todayAction().triggerConditions().isEmpty());
        assertFalse(report.marketRegime().counterEvidence().isEmpty());
    }

    @Test void expiredSnapshotCannotProduceDefinitiveAction() {
        var source = snapshot(true);
        var market = marketCalculator.calculate(source, history(), indices(130, false));
        var portfolio = portfolioCalculator.calculate(List.of(), Map.of(), new JSONObject(), market);
        var report = analyzer.analyze(market, portfolio, source, now.plusMinutes(16));
        assertTrue(report.dataQuality().stale());
        assertEquals("UNKNOWN", report.marketRegime().riskLevel());
        assertEquals("WAIT_CONFIRMATION", report.todayAction().label());
    }

    @Test void cachedReportLosesDefinitiveRiskWhenItAgesOrRefreshFails() {
        var source = snapshot(true);
        var market = marketCalculator.calculate(source, history(), indices(130, false));
        var portfolio = portfolioCalculator.calculate(List.of(), Map.of(), new JSONObject(), market);
        var report = analyzer.analyze(market, portfolio, source, now);
        assertEquals("LOW", report.marketRegime().riskLevel());
        var old = analyzer.refreshQuality(report, now.plusMinutes(16), false);
        assertTrue(old.dataQuality().stale());
        assertEquals("UNKNOWN", old.marketRegime().riskLevel());
        assertEquals("WAIT_CONFIRMATION", old.todayAction().label());
        assertTrue(analyzer.refreshQuality(report, now, true).dataQuality().stale());
    }

    @Test void missingPriorSessionDoesNotTurnMultiDayIndexChangeIntoDailyReturn() {
        var data = indices(130, false);
        data.values().forEach(value -> {
            JSONObject item = (JSONObject) value;
            var records = item.getJSONArray("records");
            records.remove(records.size() - 2);
            item.set("records", records);
        });
        var market = marketCalculator.calculate(snapshot(true), history(), data);
        assertTrue(market.indices().stream().allMatch(index -> index.changePct() == null));
        var portfolio = portfolioCalculator.calculate(List.of(holding("600001.SH", "10", "100")), Map.of(), quote("600001.SH", 12, 10), market);
        assertNull(portfolio.excessReturnPct());
    }

    @Test void modelCannotAddFactsOrDuplicateSignals() {
        var allowed = List.of("BREADTH_WEAK", "CYCLE_BEAR", "PORTFOLIO_EMPTY");
        assertEquals(allowed, DailyMarketIntelligenceServiceImpl.validateHighlights("[\"BREADTH_WEAK\",\"CYCLE_BEAR\",\"PORTFOLIO_EMPTY\"]", allowed));
        assertTrue(DailyMarketIntelligenceServiceImpl.validateHighlights("[\"BREADTH_WEAK\",\"BREADTH_WEAK\",\"SELL_ALL\"]", allowed).isEmpty());
        assertTrue(DailyMarketIntelligenceServiceImpl.validateHighlights("Market will crash 20%", allowed).isEmpty());
    }
}
