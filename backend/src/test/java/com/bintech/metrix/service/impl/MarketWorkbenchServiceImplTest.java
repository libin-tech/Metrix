package com.bintech.metrix.service.impl;

import com.bintech.metrix.dto.response.PortfolioHoldingListResponse;
import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.dao.PortfolioHoldingDao;
import com.bintech.metrix.repository.entity.PortfolioHolding;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.PortfolioHoldingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketWorkbenchServiceImplTest {

    @Test
    void shouldBuildMetricsFromForwardAdjustedDailyBars() {
        StockBasicDao stockDao = mock(StockBasicDao.class);
        PortfolioHoldingDao holdingDao = mock(PortfolioHoldingDao.class);
        MarketDataService marketData = mock(MarketDataService.class);
        PortfolioHoldingService holdings = mock(PortfolioHoldingService.class);
        StockBasic stock = new StockBasic();
        stock.setTsCode("600519.SH");
        stock.setSymbol("600519");
        stock.setName("贵州茅台");
        when(stockDao.selectByTsCode("600519.SH")).thenReturn(stock);
        when(marketData.fetchRealTimeData(stock, 1L)).thenReturn(quoteResponse());
        when(marketData.fetchKlinesData(stock, 252, 1L)).thenReturn(klineResponse());
        when(holdings.getHoldings(null, null)).thenReturn(new PortfolioHoldingListResponse(List.of(), null));

        var result = new MarketWorkbenchServiceImpl(stockDao, holdingDao, marketData, holdings).load("600519.SH", 1L);

        assertEquals("贵州茅台", result.instrument().get("name"));
        assertEquals("同花顺金融数据 API", result.source());
        assertEquals("前复权", result.adjustment());
        assertEquals(new BigDecimal("0.00"), result.metrics().get("yearReturn"));
        assertEquals(new BigDecimal("12.2000"), result.metrics().get("ma5"));
        for (String key : List.of("maxDrawdown1M", "maxDrawdown3M", "maxDrawdown6M", "maxDrawdown1Y")) {
            assertNull(result.metrics().get(key));
        }
        assertNull(result.metrics().get("averageAmount20"));
        assertEquals(5, result.bars().size());
        assertEquals("2026-09-07", result.bars().getLast().get("date"));
        verify(marketData).fetchKlinesData(stock, 252, 1L);
    }

    @Test
    void shouldFailBeforeUpstreamCallsForUnknownOrInvalidInstrument() {
        StockBasicDao stockDao = mock(StockBasicDao.class);
        PortfolioHoldingDao holdingDao = mock(PortfolioHoldingDao.class);
        MarketDataService marketData = mock(MarketDataService.class);
        PortfolioHoldingService holdings = mock(PortfolioHoldingService.class);
        var service = new MarketWorkbenchServiceImpl(stockDao, holdingDao, marketData, holdings);

        assertThrows(IllegalArgumentException.class, () -> service.load("600519", 1L));
        when(stockDao.selectByTsCode("600519.SH")).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> service.load("600519.SH", 1L));
        verify(marketData, never()).fetchRealTimeData(any(), eq(1L));
        verify(marketData, never()).fetchKlinesData(any(), eq(252), eq(1L));
    }

    @Test
    void shouldReturnAnomalyReasonsWithoutInventingMissingFields() {
        StockBasicDao stockDao = mock(StockBasicDao.class);
        PortfolioHoldingDao holdingDao = mock(PortfolioHoldingDao.class);
        MarketDataService marketData = mock(MarketDataService.class);
        PortfolioHoldingService holdings = mock(PortfolioHoldingService.class);
        when(holdingDao.selectByUserId(1L)).thenReturn(List.of(holding("600519.SH")));
        when(marketData.fetchAnomalyAnalysisData(1L)).thenReturn(Map.of("status", "success", "data", Map.of(
                "timestamp", 1788710400000L,
                "item", List.of(Map.of("stock_name", "贵州茅台", "thscode", "600519.SH", "tag_name", "大涨",
                        "analysis_content", "异动原因", "keyword_list", List.of("白酒"))))));

        Map<String, Object> result = new MarketWorkbenchServiceImpl(stockDao, holdingDao, marketData, holdings).anomalies(1L);

        assertEquals("同花顺金融数据 API", result.get("source"));
        assertEquals(1, ((List<?>) result.get("items")).size());
        assertEquals("2026-09-07T00:00+08:00", result.get("dataTime"));
    }

    @Test
    void shouldFilterByAllUserHoldingsWithoutTruncationOrDuplicateAnomalies() {
        PortfolioHoldingDao holdingDao = mock(PortfolioHoldingDao.class);
        MarketDataService marketData = mock(MarketDataService.class);
        var holdings = IntStream.rangeClosed(1, 9)
                .mapToObj(index -> holding(String.format("%06d.SZ", index)))
                .collect(Collectors.toCollection(ArrayList::new));
        holdings.add(holding("000009.SZ"));
        when(holdingDao.selectByUserId(7L)).thenReturn(holdings);
        when(marketData.fetchAnomalyAnalysisData(7L)).thenReturn(Map.of("status", "success", "data", Map.of(
                "item", List.of(Map.of("thscode", "000009.SZ"), Map.of("thscode", "600519.SH")))));
        var service = new MarketWorkbenchServiceImpl(mock(StockBasicDao.class), holdingDao,
                marketData, mock(PortfolioHoldingService.class));

        assertEquals(List.of(Map.of("thscode", "000009.SZ")), service.anomalies(7L).get("items"));
        verify(holdingDao).selectByUserId(7L);
    }

    @Test
    void shouldSkipUpstreamWhenUserHasNoHoldings() {
        PortfolioHoldingDao holdingDao = mock(PortfolioHoldingDao.class);
        MarketDataService marketData = mock(MarketDataService.class);
        when(holdingDao.selectByUserId(7L)).thenReturn(List.of());
        var service = new MarketWorkbenchServiceImpl(mock(StockBasicDao.class), holdingDao,
                marketData, mock(PortfolioHoldingService.class));

        assertEquals(List.of(), service.anomalies(7L).get("items"));
        verify(marketData, never()).fetchAnomalyAnalysisData(any());
    }

    private PortfolioHolding holding(String code) {
        PortfolioHolding holding = new PortfolioHolding();
        holding.setStockCode(code);
        return holding;
    }

    @Test
    void shouldCalculateEachDrawdownWithinItsOwnTradingWindow() {
        List<Integer> closes = new ArrayList<>(Collections.nCopies(252, 100));
        closes.set(0, 200);
        closes.set(126, 160);
        closes.set(189, 125);
        closes.set(231, 110);
        Map<String, Object> metrics = metricsForCloses(closes);

        assertEquals(new BigDecimal("-9.09"), metrics.get("maxDrawdown1M"));
        assertEquals(new BigDecimal("-20.00"), metrics.get("maxDrawdown3M"));
        assertEquals(new BigDecimal("-37.50"), metrics.get("maxDrawdown6M"));
        assertEquals(new BigDecimal("-50.00"), metrics.get("maxDrawdown1Y"));
    }

    @Test
    void shouldReturnZeroForRisingPricesAndNullForIncompletePeriods() {
        Map<String, Object> metrics = metricsForCloses(IntStream.rangeClosed(1, 63).boxed().toList());
        assertEquals(new BigDecimal("0.00"), metrics.get("maxDrawdown1M"));
        assertEquals(new BigDecimal("0.00"), metrics.get("maxDrawdown3M"));
        assertNull(metrics.get("maxDrawdown6M"));
        assertNull(metrics.get("maxDrawdown1Y"));
    }

    private Map<String, Object> metricsForCloses(List<Integer> closes) {
        StockBasicDao stockDao = mock(StockBasicDao.class);
        MarketDataService marketData = mock(MarketDataService.class);
        PortfolioHoldingService holdings = mock(PortfolioHoldingService.class);
        StockBasic stock = new StockBasic();
        stock.setTsCode("600519.SH");
        stock.setSymbol("600519");
        stock.setName("贵州茅台");
        when(stockDao.selectByTsCode(stock.getTsCode())).thenReturn(stock);
        when(marketData.fetchRealTimeData(stock, 1L)).thenReturn(quoteResponse());
        List<Long> timestamps = IntStream.range(0, closes.size())
                .mapToObj(index -> 1757030400000L + index * 86400000L).toList();
        when(marketData.fetchKlinesData(stock, 252, 1L)).thenReturn(Map.of("status", "success", "data", Map.of(
                "timestamp", timestamps, "open", closes, "high", closes, "low", closes,
                "close", closes, "volume", closes, "amount", closes)));
        when(holdings.getHoldings(null, null)).thenReturn(new PortfolioHoldingListResponse(List.of(), null));
        return new MarketWorkbenchServiceImpl(stockDao, mock(PortfolioHoldingDao.class), marketData, holdings)
                .load(stock.getTsCode(), 1L).metrics();
    }

    private Map<String, Object> quoteResponse() {
        return Map.of("status", "success", "data", List.of(Map.of(
                "last_price", 13, "prev_close", 12, "open", 12.2, "high", 13.2, "low", 12.1,
                "volume", 1200, "amount", 130, "timestamp", 1788710400000L,
                "ext", Map.of("change_pct", 8.33, "change_amount", 1, "amplitude", 9.17))));
    }

    private Map<String, Object> klineResponse() {
        return Map.of("status", "success", "data", Map.of(
                "timestamp", List.of(1788364800000L, 1788451200000L, 1788537600000L, 1788624000000L, 1788710400000L),
                "open", List.of(10, 13, 12, 12, 12), "high", List.of(13, 13, 13, 13, 13.2),
                "low", List.of(10, 12, 11, 12, 12.1), "close", List.of(13, 12, 11, 12, 13),
                "volume", List.of(100, 100, 100, 100, 100), "amount", List.of(110, 120, 130, 140, 150)));
    }
}
