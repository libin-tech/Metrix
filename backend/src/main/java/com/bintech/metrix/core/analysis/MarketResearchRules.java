package com.bintech.metrix.core.analysis;

import com.bintech.metrix.dto.response.DailyMarketIntelligence.IndexView;
import com.bintech.metrix.dto.response.DailyMarketIntelligence.MarketSnapshot;
import java.util.List;
import java.util.function.Predicate;

import static com.bintech.metrix.constants.MarketIntelligenceConstants.*;

/** 有序规则策略：日内强弱、情绪与中期周期使用不同证据，避免单日涨跌判牛熊。 */
public final class MarketResearchRules {
    private MarketResearchRules() {}
    public record Rule<T>(String label, Predicate<T> matches) {}

    public static <T> String classify(T value, List<Rule<T>> rules, String fallback) {
        return rules.stream().filter(rule -> rule.matches().test(value)).map(Rule::label).findFirst().orElse(fallback);
    }

    public static String breadth(Double upPct) {
        if (upPct == null) return "UNKNOWN";
        return classify(upPct, List.of(new Rule<>("STRONG", value -> value >= STRONG_BREADTH),
                new Rule<>("WEAK", value -> value <= WEAK_BREADTH)), "RANGE");
    }

    public static String cycle(List<IndexView> indices) {
        List<IndexView> covered = indices.stream().filter(index -> index.ma120() != null && index.slope120() != null && index.completedClose() != null && index.ma60() != null && index.slope60() != null).toList();
        if (covered.size() < MIN_CYCLE_INDICES) return "INSUFFICIENT";
        return classify(covered, List.of(
                new Rule<>("BEAR", list -> countTrend(list, "DOWN") >= MIN_CYCLE_INDICES),
                new Rule<>("BULL", list -> countTrend(list, "UP") >= MIN_CYCLE_INDICES),
                new Rule<>("BEAR_RISK", list -> list.stream().filter(index -> index.completedClose() < index.ma60() && index.slope60() < 0).count() >= MIN_CYCLE_INDICES),
                new Rule<>("BULL_DIVERGENCE", list -> list.stream().filter(index -> index.completedClose() > index.ma120() && index.slope120() > 0).count() >= MIN_CYCLE_INDICES)
        ), "RANGE");
    }

    private static long countTrend(List<IndexView> indices, String trend) {
        return indices.stream().filter(index -> trend.equals(index.trend())).count();
    }

    public static String sentiment(MarketSnapshot market) {
        if (market.upPct() == null || market.limitDown() == null || market.limitUp() == null) return "UNKNOWN";
        long falling = market.indices().stream().filter(index -> market.date().equals(index.date())
                && index.changePct() != null && index.changePct() < 0).count();
        boolean downsideLimits = market.limitDown() != null && market.limitUp() != null && market.limitDown() > market.limitUp();
        boolean upsideLimits = market.limitDown() != null && market.limitUp() != null && market.limitUp() > market.limitDown();
        return classify(market.upPct(), List.of(
                new Rule<>("EXTREME_PESSIMISM", value -> value <= EXTREME_BREADTH && downsideLimits && falling >= MIN_CYCLE_INDICES),
                new Rule<>("PESSIMISM", value -> value <= WEAK_BREADTH && (downsideLimits || falling >= MIN_CYCLE_INDICES)),
                new Rule<>("OVERHEATED", value -> value >= HOT_BREADTH && upsideLimits && market.turnoverVs20Pct() != null && market.turnoverVs20Pct() >= VOLUME_CHANGE),
                new Rule<>("OPTIMISM", value -> value >= STRONG_BREADTH && upsideLimits)
        ), "NEUTRAL");
    }
}
