package com.bintech.metrix.core.scheduled;

import com.bintech.metrix.service.DailyMarketIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicBoolean;
import static com.bintech.metrix.constants.MarketIntelligenceConstants.*;

/** 无需用户在线也留存收盘样本；交易日和闭市状态由数据源日历再次验证。 */
@Component
@RequiredArgsConstructor
public class MarketIntelligenceScheduledTask {
    private final DailyMarketIntelligenceService service;
    private final AtomicBoolean running = new AtomicBoolean();

    @Scheduled(cron = ARCHIVE_CRON, zone = MARKET_TIME_ZONE)
    public void archiveClose() {
        if (!running.compareAndSet(false, true)) return;
        Thread.startVirtualThread(() -> {
            try { service.refreshMarketHistory(); }
            finally { running.set(false); }
        });
    }
}
