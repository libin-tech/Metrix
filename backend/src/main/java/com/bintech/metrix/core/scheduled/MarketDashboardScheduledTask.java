package com.bintech.metrix.core.scheduled;

import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.websocket.MarketDashboardWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 首页市场工作台推送任务。
 */
@Component
@RequiredArgsConstructor
public class MarketDashboardScheduledTask {

    private final MarketDashboardWebSocketHandler marketDashboardWebSocketHandler;

    /**
     * 每五分钟更新市场快照及当天成交额。
     */
    @Scheduled(fixedDelay = SystemConstants.MARKET_TURNOVER_REFRESH_INTERVAL_MILLIS,
            initialDelay = SystemConstants.MARKET_TURNOVER_REFRESH_INTERVAL_MILLIS)
    public void refreshOverview() {
        marketDashboardWebSocketHandler.refreshOverviewAndBroadcast();
    }

    /**
     * 每三十分钟更新龙虎榜、板块和各股票池。
     */
    @Scheduled(fixedDelay = SystemConstants.MARKET_INSIGHTS_REFRESH_INTERVAL_MILLIS,
            initialDelay = SystemConstants.MARKET_INSIGHTS_REFRESH_INTERVAL_MILLIS)
    public void refreshInsights() {
        marketDashboardWebSocketHandler.refreshInsightsAndBroadcast();
    }
}
