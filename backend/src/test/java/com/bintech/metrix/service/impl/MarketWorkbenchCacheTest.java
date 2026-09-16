package com.bintech.metrix.service.impl;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.bintech.metrix.constants.CacheConstants;
import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.MarketTradingSession;
import com.bintech.metrix.service.RedisCacheService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketWorkbenchCacheTest {

    private static final ZoneId MARKET_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String CACHE_DATE_KEY = "cacheDate";
    private static final String CACHE_PAYLOAD_KEY = "payload";

    @Test
    void shouldRefreshIntradayWhenCachedDataIsFromPreviousDay() {
        String thscode = "600519.SH";
        String today = LocalDate.now(MARKET_ZONE).toString();
        String previousDay = LocalDate.now(MARKET_ZONE).minusDays(1).toString();
        StockBasic stock = new StockBasic();
        stock.setTsCode(thscode);
        Map<String, String> cache = new HashMap<>();
        cache.put(CacheConstants.MARKET_WORKBENCH_INTRADAY_KEY_PREFIX + thscode,
                JSONUtil.toJsonStr(intradayResponse(previousDay)));
        AtomicBoolean upstreamRequested = new AtomicBoolean();
        MarketWorkbenchServiceImpl service = new MarketWorkbenchServiceImpl(createStockBasicDao(stock), null,
                createMarketDataService(upstreamRequested, intradayResponse(today)), null,
                createRedisCacheService(cache), closedSession());

        Map<String, Object> result = service.intraday(thscode);

        assertEquals(today, result.get("date"));
        assertTrue(upstreamRequested.get());
        JSONObject cacheEnvelope = JSONUtil.parseObj(cache.get(CacheConstants.MARKET_WORKBENCH_INTRADAY_KEY_PREFIX + thscode));
        assertEquals(today, cacheEnvelope.getStr(CACHE_DATE_KEY));
        assertEquals(today, cacheEnvelope.getJSONObject(CACHE_PAYLOAD_KEY).getJSONObject("data").getStr("date"));
    }

    private StockBasicDao createStockBasicDao(StockBasic stock) {
        return StockBasicDao.class.cast(Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{StockBasicDao.class}, (proxy, method, arguments) -> stock));
    }

    private MarketDataService createMarketDataService(AtomicBoolean upstreamRequested, Map<String, Object> response) {
        return MarketDataService.class.cast(Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{MarketDataService.class}, (proxy, method, arguments) -> {
                    if ("fetchIntradayData".equals(method.getName())) {
                        upstreamRequested.set(true);
                        return response;
                    }
                    return null;
                }));
    }

    private RedisCacheService createRedisCacheService(Map<String, String> cache) {
        return RedisCacheService.class.cast(Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{RedisCacheService.class}, (proxy, method, arguments) -> {
                    if ("get".equals(method.getName())) {
                        return cache.get(arguments[0]);
                    }
                    if ("setJson".equals(method.getName())) {
                        cache.put((String) arguments[0], JSONUtil.toJsonStr(arguments[1]));
                    }
                    return null;
                }));
    }

    private MarketTradingSession closedSession() {
        return new MarketTradingSession() {
            @Override
            public boolean isTrading() {
                return false;
            }
        };
    }

    private Map<String, Object> intradayResponse(String date) {
        return Map.of("status", "success", "data", Map.of("date", date, "points", List.of()));
    }
}
