package com.bintech.metrix.core.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketReviewPromptBuilderTest {

    @Test
    void shouldIncludeSixtyDayMarketTurnoverHistoryInPrompt() {
        MarketReviewPromptBuilder builder = new MarketReviewPromptBuilder();
        Map<String, Object> indexData = Map.of();
        Map<String, Object> marketTurnoverData = Map.of("history", List.of(
                Map.of("date", "2026-06-01", "volume", 10L, "amount", 100L),
                Map.of("date", "2026-08-31", "volume", 20L, "amount", 200L)));

        String prompt = builder.build(indexData, marketTurnoverData, "2026-08-31");

        assertTrue(prompt.contains("【市场近60个交易日量能数据】"));
        assertTrue(prompt.contains("2026-06-01 | 10 | 100"));
        assertTrue(prompt.contains("2026-08-31 | 20 | 200"));
    }
}
