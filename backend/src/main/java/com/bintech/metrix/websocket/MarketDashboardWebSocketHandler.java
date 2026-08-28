package com.bintech.metrix.websocket;

import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.CacheConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.service.MarketActivityService;
import com.bintech.metrix.service.MarketIndexService;
import com.bintech.metrix.service.MarketInsightService;
import com.bintech.metrix.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 为首页网格提供市场快照和市场洞察的统一 WebSocket 推送。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDashboardWebSocketHandler extends TextWebSocketHandler {

    private final MarketActivityService marketActivityService;
    private final MarketIndexService marketIndexService;
    private final MarketInsightService marketInsightService;
    private final RedisCacheService redisCacheService;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        Long userId = getUserId(session);
        if (userId != null) {
            Thread.ofVirtual().name("market-dashboard-initial-push").start(() -> pushInitialSnapshot(session, userId));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session.getId());
        log.warn("市场工作台 WebSocket 连接异常: {}", exception.getMessage());
    }

    /**
     * 向所有订阅者推送市场快照和当天成交额。
     */
    public synchronized void refreshOverviewAndBroadcast() {
        if (sessions.isEmpty()) {
            return;
        }
        Map<String, Object> overviewCommonData = fetchOverviewCommonData();
        sessions.values().forEach(session -> sendOverview(session, getUserId(session), overviewCommonData));
    }

    /**
     * 向所有订阅者推送龙虎榜、板块及股票池数据。
     */
    public synchronized void refreshInsightsAndBroadcast() {
        if (sessions.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> insights = extractData(marketInsightService.getMarketInsights());
            cacheData(CacheConstants.MARKET_DASHBOARD_INSIGHTS_LAST_SUCCESS_KEY, insights);
            sessions.values().forEach(session -> send(session,
                    createMessage(SystemConstants.MARKET_DASHBOARD_INSIGHTS_MESSAGE_TYPE, insights)));
        } catch (RuntimeException exception) {
            log.warn("刷新市场工作台洞察数据失败: {}", exception.getMessage());
        }
    }

    private void pushInitialSnapshot(WebSocketSession session, Long userId) {
        sendCachedOverview(session, userId);
        sendCachedInsights(session);
        if (requiresOverviewInitialization(userId)) {
            refreshOverviewAndBroadcast();
        }
        if (getCachedData(CacheConstants.MARKET_DASHBOARD_INSIGHTS_LAST_SUCCESS_KEY) == null) {
            refreshInsightsAndBroadcast();
        }
    }

    private Map<String, Object> fetchOverviewCommonData() {
        Map<String, Object> overview = new HashMap<>();
        putOverviewData(overview, SystemConstants.MARKET_DASHBOARD_ACTIVITY_KEY,
                CacheConstants.MARKET_DASHBOARD_ACTIVITY_LAST_SUCCESS_KEY, marketActivityService::getMarketActivity);
        putOverviewData(overview, SystemConstants.MARKET_DASHBOARD_INDEX_KEY,
                CacheConstants.MARKET_DASHBOARD_INDEX_LAST_SUCCESS_KEY, marketIndexService::getMarketIndex);
        return overview;
    }

    private void sendOverview(WebSocketSession session, Long userId, Map<String, Object> overviewCommonData) {
        if (userId == null) {
            return;
        }
        Map<String, Object> overview = new HashMap<>(overviewCommonData);
        putOverviewData(overview, SystemConstants.MARKET_DASHBOARD_TURNOVER_KEY,
                null, () -> marketIndexService.getMarketTurnover(userId));
        if (!overview.isEmpty()) {
            send(session, createMessage(SystemConstants.MARKET_DASHBOARD_OVERVIEW_MESSAGE_TYPE, overview));
        }
    }

    private void putOverviewData(Map<String, Object> overview, String dataKey,
                                 String cacheKey, Supplier<Map<String, Object>> dataSupplier) {
        try {
            Map<String, Object> data = extractData(dataSupplier.get());
            overview.put(dataKey, data);
            if (cacheKey != null) {
                cacheData(cacheKey, data);
            }
        } catch (RuntimeException exception) {
            log.warn("获取市场工作台{}数据失败: {}", dataKey, exception.getMessage());
        }
    }

    private void sendCachedOverview(WebSocketSession session, Long userId) {
        Map<String, Object> overview = getCachedOverviewCommonData();
        putCachedTurnoverData(overview, userId);
        if (!overview.isEmpty()) {
            send(session, createMessage(SystemConstants.MARKET_DASHBOARD_OVERVIEW_MESSAGE_TYPE, overview));
        }
    }

    private void putCachedTurnoverData(Map<String, Object> overview, Long userId) {
        try {
            Map<String, Object> turnover = marketIndexService.getCachedMarketTurnover(userId);
            if (turnover != null) {
                overview.put(SystemConstants.MARKET_DASHBOARD_TURNOVER_KEY, extractData(turnover));
            }
        } catch (RuntimeException exception) {
            log.warn("读取市场成交额缓存失败: {}", exception.getMessage());
        }
    }

    private void sendCachedInsights(WebSocketSession session) {
        Map<String, Object> insights = getCachedData(CacheConstants.MARKET_DASHBOARD_INSIGHTS_LAST_SUCCESS_KEY);
        if (insights != null) {
            send(session, createMessage(SystemConstants.MARKET_DASHBOARD_INSIGHTS_MESSAGE_TYPE, insights));
        }
    }

    private boolean requiresOverviewInitialization(Long userId) {
        return getCachedData(CacheConstants.MARKET_DASHBOARD_ACTIVITY_LAST_SUCCESS_KEY) == null
                || getCachedData(CacheConstants.MARKET_DASHBOARD_INDEX_LAST_SUCCESS_KEY) == null
                || marketIndexService.getCachedMarketTurnover(userId) == null;
    }

    private Map<String, Object> getCachedOverviewCommonData() {
        Map<String, Object> overview = new HashMap<>();
        putCachedOverviewData(overview, SystemConstants.MARKET_DASHBOARD_ACTIVITY_KEY,
                CacheConstants.MARKET_DASHBOARD_ACTIVITY_LAST_SUCCESS_KEY);
        putCachedOverviewData(overview, SystemConstants.MARKET_DASHBOARD_INDEX_KEY,
                CacheConstants.MARKET_DASHBOARD_INDEX_LAST_SUCCESS_KEY);
        return overview;
    }

    private void putCachedOverviewData(Map<String, Object> overview, String dataKey, String cacheKey) {
        Map<String, Object> cachedData = getCachedData(cacheKey);
        if (cachedData != null) {
            overview.put(dataKey, cachedData);
        }
    }

    private void cacheData(String cacheKey, Map<String, Object> data) {
        redisCacheService.setJson(cacheKey, data);
    }

    private Map<String, Object> getCachedData(String cacheKey) {
        String cachedValue = redisCacheService.get(cacheKey);
        if (cachedValue == null || cachedValue.isBlank()) {
            return null;
        }
        try {
            return new HashMap<>(JSONUtil.parseObj(cachedValue));
        } catch (RuntimeException exception) {
            log.warn("市场工作台缓存数据无效: {}", exception.getMessage());
            return null;
        }
    }

    private Map<String, Object> createMessage(String messageType, Map<String, Object> data) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", messageType);
        message.put(ApiConstants.KEY_DATA, data);
        return message;
    }

    private Map<String, Object> extractData(Map<String, Object> sourceData) {
        JSONObject source = JSONUtil.parseObj(sourceData);
        JSONObject data = source.getJSONObject(ApiConstants.KEY_DATA);
        if (!ApiConstants.STATUS_SUCCESS.equals(source.getStr(ApiConstants.KEY_STATUS)) || data == null) {
            throw new RuntimeException("市场工作台上游数据无效");
        }
        return new HashMap<>(data);
    }

    private void send(WebSocketSession session, Map<String, Object> message) {
        if (!session.isOpen()) {
            sessions.remove(session.getId());
            return;
        }
        synchronized (session) {
            try {
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(message)));
            } catch (IOException exception) {
                sessions.remove(session.getId());
                log.warn("推送市场工作台数据失败: {}", exception.getMessage());
            }
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object loginId = session.getAttributes().get("loginId");
        if (loginId == null) {
            sessions.remove(session.getId());
            log.warn("市场工作台 WebSocket 缺少登录用户信息");
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(loginId));
        } catch (NumberFormatException exception) {
            sessions.remove(session.getId());
            log.warn("市场工作台 WebSocket 用户标识无效: {}", loginId);
            return null;
        }
    }
}
