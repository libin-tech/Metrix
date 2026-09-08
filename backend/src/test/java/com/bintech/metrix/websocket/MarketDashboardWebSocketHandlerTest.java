package com.bintech.metrix.websocket;

import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.service.MarketActivityService;
import com.bintech.metrix.service.MarketIndexService;
import com.bintech.metrix.service.MarketInsightService;
import com.bintech.metrix.service.RedisCacheService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketDashboardWebSocketHandlerTest {
    @Test
    void shouldRefreshOverviewAfterSendingExistingCacheOnConnection() throws Exception {
        MarketActivityService activity = mock(MarketActivityService.class);
        MarketIndexService index = mock(MarketIndexService.class);
        MarketInsightService insights = mock(MarketInsightService.class);
        RedisCacheService cache = mock(RedisCacheService.class);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("reconnected-home");
        when(session.getAttributes()).thenReturn(Map.of("loginId", 7L));
        when(session.isOpen()).thenReturn(true);
        when(cache.get(anyString())).thenReturn("{\"date\":\"2026-09-07\"}");
        Map<String, Object> response = Map.of(ApiConstants.KEY_STATUS, ApiConstants.STATUS_SUCCESS,
                ApiConstants.KEY_DATA, Map.of("date", "2026-09-08"));
        when(index.getCachedMarketTurnover(7L)).thenReturn(response);
        when(activity.getMarketActivity()).thenReturn(response);
        when(index.getMarketIndex()).thenReturn(response);
        when(index.getMarketTurnover(7L)).thenReturn(response);
        when(insights.getMarketInsights()).thenReturn(response);

        new MarketDashboardWebSocketHandler(activity, index, insights, cache).afterConnectionEstablished(session);

        verify(activity, timeout(1000)).getMarketActivity();
        verify(index, timeout(1000)).getMarketTurnover(7L);
        verify(insights, timeout(1000)).getMarketInsights();
        ArgumentCaptor<TextMessage> messages = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, timeout(1000).times(4)).sendMessage(messages.capture());
        assertTrue(messages.getAllValues().getFirst().getPayload().contains("2026-09-07"));
        assertTrue(messages.getAllValues().getLast().getPayload().contains("2026-09-08"));
    }
}
