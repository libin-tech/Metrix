package com.bintech.metrix.core.analysis;

import cn.hutool.json.JSONObject;
import com.bintech.metrix.dto.response.DailyMarketIntelligence.*;
import com.bintech.metrix.repository.entity.PortfolioHolding;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import static com.bintech.metrix.constants.MarketIntelligenceConstants.*;
import static com.bintech.metrix.core.analysis.IntelligenceNumbers.*;

/** 当前数量固定的持仓快照估值，不冒充含交易、现金、分红及费用的账户收益率。 */
@Component
public class PortfolioSnapshotCalculator {
    /** 任一持仓报价缺失则组合估值留空，不把不完整组合当作完整结果。 */
    public PortfolioSnapshot calculate(List<PortfolioHolding> positions, Map<String, String> industries,
            JSONObject quotes, MarketSnapshot market) {
        List<HoldingView> rows = positions.stream().filter(item -> item.getQuantity() == null || item.getQuantity().signum() != 0)
                .map(item -> holding(item, industries, quotes)).toList();
        BigDecimal cost = sum(rows, HoldingView::totalCost);
        BigDecimal value = sum(rows, HoldingView::marketValue);
        BigDecimal pnl = subtract(value, cost);
        BigDecimal daily = sum(rows, HoldingView::dailyPnl);
        BigDecimal previousValue = subtract(value, daily);
        BigDecimal dailyReturn = percent(daily, previousValue);
        Double benchmark = market.indices().stream().filter(index -> "sh000300".equals(index.symbol())
                && market.date().equals(index.date())).map(IndexView::changePct).filter(item -> item != null).findFirst().orElse(null);
        BigDecimal excess = dailyReturn == null || benchmark == null ? null : dailyReturn.subtract(BigDecimal.valueOf(benchmark));
        BigDecimal stockConcentration = concentration(rows, value, HoldingView::stockCode);
        BigDecimal industryConcentration = concentration(rows, value, HoldingView::industry);
        String relative = excess == null ? "UNKNOWN" : MarketResearchRules.classify(excess, List.of(
                new MarketResearchRules.Rule<>("UNDERPERFORM", item -> item.doubleValue() <= -RELATIVE_RETURN_GAP),
                new MarketResearchRules.Rule<>("OUTPERFORM", item -> item.doubleValue() >= RELATIVE_RETURN_GAP)), "IN_LINE");
        boolean concentrated = above(stockConcentration, STOCK_CONCENTRATION) || above(industryConcentration, INDUSTRY_CONCENTRATION);
        String exposure = MarketResearchRules.classify(rows, List.of(
                new MarketResearchRules.Rule<>("EMPTY", List::isEmpty),
                new MarketResearchRules.Rule<>("UNKNOWN", items -> value == null || industryConcentration == null),
                new MarketResearchRules.Rule<>("CONCENTRATED", items -> concentrated)), "NORMAL");
        List<HoldingView> weighted = rows.stream().map(row -> new HoldingView(row.stockCode(), row.stockName(), row.industry(),
                row.quantity(), row.totalCost(), row.price(), row.prevClose(), row.marketValue(), row.floatingPnl(), row.dailyPnl(),
                percent(row.marketValue(), value), row.quoteStatus())).toList();
        return new PortfolioSnapshot(rows.size(), cost, value, pnl, percent(pnl, cost), daily, dailyReturn,
                "sh000300", excess, relative, exposure, stockConcentration, industryConcentration, weighted);
    }

    private HoldingView holding(PortfolioHolding item, Map<String, String> industries, JSONObject quotes) {
        JSONObject quote = quotes == null ? null : quotes.getJSONObject(item.getStockCode());
        BigDecimal price = quote == null ? null : decimal(quote.get("price"));
        BigDecimal previous = quote == null ? null : decimal(quote.get("prevClose"));
        BigDecimal quantity = item.getQuantity();
        BigDecimal cost = item.getCost();
        boolean validQuantity = quantity != null && quantity.signum() > 0;
        BigDecimal totalCost = validQuantity && cost != null && cost.signum() >= 0 ? cost.multiply(quantity) : null;
        BigDecimal value = validQuantity && price != null ? price.multiply(quantity) : null;
        BigDecimal daily = validQuantity && price != null && previous != null ? price.subtract(previous).multiply(quantity) : null;
        return new HoldingView(item.getStockCode(), item.getStockName(), industries.get(item.getStockCode()), quantity,
                totalCost, price, previous, value, subtract(value, totalCost), daily, null, daily == null ? "MISSING" : "AVAILABLE");
    }

    private BigDecimal decimal(Object value) {
        Double number = positive(value);
        return number == null ? null : new BigDecimal(value.toString());
    }

    private BigDecimal subtract(BigDecimal first, BigDecimal second) {
        return first == null || second == null ? null : first.subtract(second);
    }

    private BigDecimal sum(List<HoldingView> rows, Function<HoldingView, BigDecimal> field) {
        if (rows.stream().anyMatch(row -> field.apply(row) == null)) return null;
        return rows.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 同一股票跨账户合并后计算集中度，避免拆分账户稀释风险。 */
    private BigDecimal concentration(List<HoldingView> rows, BigDecimal total, Function<HoldingView, String> key) {
        if (total == null || total.signum() <= 0 || rows.stream().anyMatch(row -> key.apply(row) == null || key.apply(row).isBlank())) return null;
        Map<String, BigDecimal> groups = new HashMap<>();
        rows.forEach(row -> groups.merge(key.apply(row), row.marketValue(), BigDecimal::add));
        return percent(groups.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO), total);
    }

    public static boolean above(BigDecimal value, double threshold) {
        return value != null && value.compareTo(BigDecimal.valueOf(threshold)) >= 0;
    }
}
