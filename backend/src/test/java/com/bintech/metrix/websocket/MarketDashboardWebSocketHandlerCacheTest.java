package com.bintech.metrix.websocket;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.CacheConstants;
import com.bintech.metrix.service.MarketInsightService;
import com.bintech.metrix.service.RedisCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketDashboardWebSocketHandlerCacheTest {

    @Test
    void shouldKeepCachedFundFlowAndPopularityRankWhenLatestResponseIsEmpty() throws Exception {
        Map<String, String> cache = new HashMap<>();
        cache.put(CacheConstants.MARKET_DASHBOARD_INSIGHTS_LAST_SUCCESS_KEY,
                JSONUtil.toJsonStr(createCachedInsights()));
        RedisCacheService redisCacheService = createRedisCacheService(cache);
        MarketInsightService marketInsightService = createMarketInsightService(createEmptyInsightsResponse());
        MarketDashboardWebSocketHandler handler = new MarketDashboardWebSocketHandler(null, null,
                marketInsightService, redisCacheService);
        List<String> messages = new ArrayList<>();
        registerSession(handler, createOpenSession(messages));

        handler.refreshInsightsAndBroadcast();

        JSONObject insights = JSONUtil.parseObj(cache.get(CacheConstants.MARKET_DASHBOARD_INSIGHTS_LAST_SUCCESS_KEY));
        assertEquals(1, insights.getJSONArray("fundFlow").size());
        assertEquals(1, insights.getJSONArray("popularityRank").size());
        assertTrue(messages.getFirst().contains("上次资金流向"));
    }

    private Map<String, Object> createCachedInsights() {
        return Map.of(
                "fundFlow", List.of(Map.of("name", "上次资金流向")),
                "popularityRank", List.of(Map.of("name", "上次人气榜")),
                "dragonTiger", List.of(),
                "industrySectors", List.of(),
                "limitPools", Map.of());
    }

    private Map<String, Object> createEmptyInsightsResponse() {
        return Map.of(ApiConstants.KEY_STATUS, ApiConstants.STATUS_SUCCESS,
                ApiConstants.KEY_DATA, Map.of(
                        "fundFlow", List.of(),
                        "popularityRank", List.of(),
                        "dragonTiger", List.of(),
                        "industrySectors", List.of(),
                        "limitPools", Map.of()));
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

    private MarketInsightService createMarketInsightService(Map<String, Object> response) {
        return MarketInsightService.class.cast(Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{MarketInsightService.class}, (proxy, method, arguments) -> response));
    }

    private WebSocketSession createOpenSession(List<String> messages) {
        return WebSocketSession.class.cast(Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{WebSocketSession.class}, (proxy, method, arguments) -> {
                    if ("getId".equals(method.getName())) {
                        return "insights-cache";
                    }
                    if ("isOpen".equals(method.getName())) {
                        return true;
                    }
                    if ("sendMessage".equals(method.getName())) {
                        messages.add(((TextMessage) arguments[0]).getPayload());
                        return null;
                    }
                    return null;
                }));
    }

    private void registerSession(MarketDashboardWebSocketHandler handler, WebSocketSession session)
            throws ReflectiveOperationException {
        Field sessionsField = MarketDashboardWebSocketHandler.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        Object sessions = sessionsField.get(handler);
        sessions.getClass().getMethod("put", Object.class, Object.class).invoke(sessions, session.getId(), session);
    }
}
