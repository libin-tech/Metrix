package com.bintech.metrix.service.impl;

import cn.hutool.json.JSONObject;
import com.bintech.metrix.constants.CacheConstants;
import com.bintech.metrix.core.analysis.*;
import com.bintech.metrix.repository.dao.PortfolioHoldingDao;
import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.entity.PortfolioHolding;
import com.bintech.metrix.service.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import static com.bintech.metrix.constants.MarketIntelligenceConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

class DailyMarketIntelligenceServiceTest {
    @Test void isolatesUserHoldingsWhileSharingMarketFetchAndDeduplicatingRefresh() {
        var activity = mock(MarketActivityService.class);
        var cache = mock(RedisCacheService.class);
        var fetcher = mock(MarketReviewDataFetcher.class);
        var holdings = mock(PortfolioHoldingDao.class);
        var stocks = mock(StockBasicDao.class);
        var ai = mock(AiModelService.class);
        JSONObject source = new JSONObject().set("statDate", "2026-09-15").set("previousTradeDate", "2026-09-14")
                .set("scope", SCOPE).set("sessionPhase", "CLOSED").set("fetchedAt", OffsetDateTime.now(ZONE).toString())
                .set("up", 100).set("down", 100).set("flat", 1).set("totalTurnover", 10000)
                .set("quotes", new JSONObject().set("600001.SH", new JSONObject().set("price", 12).set("prevClose", 10)));
        when(activity.getMarketActivity()).thenReturn(Map.of("data", Map.of("stale", false)));
        when(cache.get(CacheConstants.MARKET_RESEARCH_QUOTES_KEY)).thenReturn(source.toString());
        when(fetcher.fetchIndexData(anyString(), anyMap(), anyInt())).thenReturn(Map.of());
        var item = new PortfolioHolding();
        item.setStockCode("600001.SH"); item.setQuantity(BigDecimal.TEN); item.setCost(BigDecimal.TEN);
        when(holdings.selectByUserId(1L)).thenReturn(List.of(item));
        when(holdings.selectByUserId(2L)).thenReturn(List.of());
        var service = new DailyMarketIntelligenceServiceImpl(activity, cache, fetcher, holdings, stocks,
                new MarketSnapshotCalculator(), new PortfolioSnapshotCalculator(), new DailyMarketAnalyzer(), ai);
        for (int i = 0; i < 5; i++) service.getReport(1L);
        service.getReport(2L);
        await().atMost(Duration.ofSeconds(3)).until(() -> service.getReport(1L).report() != null && service.getReport(2L).report() != null);
        assertEquals(1, service.getReport(1L).report().portfolioSnapshot().holdingCount());
        assertEquals(0, service.getReport(2L).report().portfolioSnapshot().holdingCount());
        assertEquals(0, service.getReport(1L).report().portfolioSnapshot().marketValue().compareTo(new BigDecimal("120")));
        verify(holdings, times(1)).selectByUserId(1L);
        verify(holdings, times(1)).selectByUserId(2L);
        verify(activity, times(1)).getMarketActivity();
        assertThrows(IllegalArgumentException.class, () -> service.getReport(null));
    }

    @Test void failedSourceReturnsExplicitErrorWithoutInventingAReport() {
        var activity = mock(MarketActivityService.class);
        when(activity.getMarketActivity()).thenThrow(new IllegalStateException("Test source unavailable"));
        var service = new DailyMarketIntelligenceServiceImpl(activity, mock(RedisCacheService.class), mock(MarketReviewDataFetcher.class),
                mock(PortfolioHoldingDao.class), mock(StockBasicDao.class), new MarketSnapshotCalculator(),
                new PortfolioSnapshotCalculator(), new DailyMarketAnalyzer(), mock(AiModelService.class));
        service.getReport(1L);
        await().atMost(Duration.ofSeconds(3)).until(() -> !service.getReport(1L).refreshing());
        assertEquals("ERROR", service.getReport(1L).status());
        assertNull(service.getReport(1L).report());
    }
}
