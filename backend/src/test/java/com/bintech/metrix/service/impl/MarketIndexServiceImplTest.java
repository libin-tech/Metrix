package com.bintech.metrix.service.impl;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.CacheConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.config.MarketTurnoverProperties;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.RedisCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketIndexServiceImplTest {

    @Test
    @DisplayName("跨交易日时不能因刷新时间较新而直接返回昨日成交额缓存")
    void shouldRefreshTurnoverWhenCachedDataIsFromPreviousDay() {
        Long userId = 1L;
        String today = LocalDate.now(ZoneId.of(SystemConstants.MARKET_TIME_ZONE_ID)).toString();
        String previousDay = LocalDate.now(ZoneId.of(SystemConstants.MARKET_TIME_ZONE_ID)).minusDays(1).toString();
        Map<String, String> cache = new HashMap<>();
        AtomicBoolean marketDataRequested = new AtomicBoolean();
        RedisCacheService redisCacheService = createRedisCacheService(cache);
        MarketDataService marketDataService = createMarketDataService(marketDataRequested, createTurnover(today, 200L));
        MarketIndexServiceImpl service = new MarketIndexServiceImpl(redisCacheService,
                new MarketTurnoverProperties(), marketDataService);

        cache.put(CacheConstants.MARKET_TURNOVER_LAST_SUCCESS_KEY_PREFIX + userId,
                JSONUtil.toJsonStr(createTurnover(previousDay, 100L)));
        cache.put(CacheConstants.MARKET_TURNOVER_REFRESH_AT_KEY_PREFIX + userId,
                String.valueOf(System.currentTimeMillis()));

        Map<String, Object> turnover = service.getMarketTurnover(userId);

        JSONObject data = JSONUtil.parseObj(turnover).getJSONObject(ApiConstants.KEY_DATA);
        assertEquals(200L, data.getLong("amount"));
        assertTrue(marketDataRequested.get());
    }

    @Test
    @DisplayName("非当天交易日的上游数据不能覆写已缓存的历史成交额")
    void shouldKeepCachedHistoryWhenLatestTurnoverIsNotToday() {
        String historicalDate = LocalDate.now(ZoneId.of(SystemConstants.MARKET_TIME_ZONE_ID)).minusDays(1).toString();
        Map<String, Object> cachedTurnover = createTurnover(historicalDate, 100L);
        Map<String, Object> upstreamTurnover = createTurnover(historicalDate, 200L);

        Map<String, Object> merged = ReflectionTestUtils.invokeMethod(MarketIndexServiceImpl.class,
                "mergeLatestTurnover", cachedTurnover, upstreamTurnover);

        JSONObject mergedData = JSONUtil.parseObj(merged).getJSONObject(ApiConstants.KEY_DATA);
        assertEquals(100L, mergedData.getLong("amount"));
        assertEquals(100L, mergedData.getJSONArray("history").getJSONObject(0).getLong("amount"));
    }

    @Test
    @DisplayName("当天的上游数据应更新当天缓存成交额")
    void shouldUpdateCachedTurnoverWhenLatestTurnoverIsToday() {
        String today = LocalDate.now(ZoneId.of(SystemConstants.MARKET_TIME_ZONE_ID)).toString();
        Map<String, Object> cachedTurnover = createTurnover(today, 100L);

        Map<String, Object> merged = ReflectionTestUtils.invokeMethod(MarketIndexServiceImpl.class,
                "mergeLatestTurnover", cachedTurnover, createTurnover(today, 200L));

        JSONObject mergedData = JSONUtil.parseObj(merged).getJSONObject(ApiConstants.KEY_DATA);
        assertEquals(200L, mergedData.getLong("amount"));
        assertEquals(200L, mergedData.getJSONArray("history").getJSONObject(0).getLong("amount"));
    }

    @Test
    @DisplayName("当天上游响应缺少成交额时不能覆写上一次成功数据")
    void shouldKeepCachedTurnoverWhenLatestResponseHasNoAmount() {
        String today = LocalDate.now(ZoneId.of(SystemConstants.MARKET_TIME_ZONE_ID)).toString();
        Map<String, Object> cachedTurnover = createTurnover(today, 100L);

        Map<String, Object> merged = ReflectionTestUtils.invokeMethod(MarketIndexServiceImpl.class,
                "mergeLatestTurnover", cachedTurnover, createInvalidTurnover(today));

        JSONObject mergedData = JSONUtil.parseObj(merged).getJSONObject(ApiConstants.KEY_DATA);
        assertEquals(100L, mergedData.getLong("amount"));
        assertEquals(100L, mergedData.getJSONArray("history").getJSONObject(0).getLong("amount"));
    }

    private Map<String, Object> createTurnover(String date, long amount) {
        Map<String, Object> item = new HashMap<>();
        item.put("date", date);
        item.put("amount", amount);

        Map<String, Object> data = new HashMap<>();
        data.put("amount", amount);
        data.put("difference", 0L);
        data.put("history", List.of(item));

        Map<String, Object> result = new HashMap<>();
        result.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_SUCCESS);
        result.put(ApiConstants.KEY_DATA, data);
        return result;
    }

    private Map<String, Object> createInvalidTurnover(String date) {
        Map<String, Object> item = new HashMap<>();
        item.put("date", date);

        Map<String, Object> data = new HashMap<>();
        data.put("history", List.of(item));

        Map<String, Object> result = new HashMap<>();
        result.put(ApiConstants.KEY_STATUS, ApiConstants.STATUS_SUCCESS);
        result.put(ApiConstants.KEY_DATA, data);
        return result;
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

    private MarketDataService createMarketDataService(AtomicBoolean marketDataRequested,
                                                       Map<String, Object> latestTurnover) {
        return MarketDataService.class.cast(Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{MarketDataService.class}, (proxy, method, arguments) -> {
                    if ("fetchMarketTurnoverData".equals(method.getName())) {
                        marketDataRequested.set(true);
                        return latestTurnover;
                    }
                    return null;
                }));
    }

}
