package com.bintech.metrix.config;

import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.websocket.MarketDashboardWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 首页市场工作台 WebSocket 配置。
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class MarketDashboardWebSocketConfig implements WebSocketConfigurer {

    private final MarketDashboardWebSocketHandler marketDashboardWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(marketDashboardWebSocketHandler, SystemConstants.MARKET_DASHBOARD_WEBSOCKET_PATH)
                .addInterceptors(new MarketDashboardHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }

    private static final class MarketDashboardHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler webSocketHandler, Map<String, Object> attributes) {
            String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams()
                    .getFirst(SystemConstants.MARKET_DASHBOARD_WEBSOCKET_TOKEN_PARAMETER);
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put("loginId", loginId);
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler webSocketHandler, Exception exception) {
        }
    }
}
