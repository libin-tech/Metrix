package com.bintech.metrix.core.scheduled;

import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.service.StockAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockAnalysisScheduledTask {

    private final StockAnalysisService stockAnalysisService;

    /**
     * 定时任务：每天凌晨3:00清理过量的股票分析记录
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void scheduledCleanup() {
        log.info("开始定时清理股票分析记录，仅保留最近{}条", BusinessConstants.SCHEDULED_CLEANUP_KEEP_COUNT);
        stockAnalysisService.cleanupExcessRecords();
        log.info("定时清理完成");
    }
}
