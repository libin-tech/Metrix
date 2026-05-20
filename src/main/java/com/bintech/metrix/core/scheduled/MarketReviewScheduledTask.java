package com.bintech.metrix.core.scheduled;

import com.bintech.metrix.service.MarketReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketReviewScheduledTask {

    private final MarketReviewService marketReviewService;

    @Scheduled(cron = "0 30 15 * * MON-FRI")
    @Transactional
    public void scheduledReview() {
        log.info("定时任务：开始大盘复盘");
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        try {
            marketReviewService.createReview(today);
            log.info("定时大盘复盘任务已提交: {}", today);
        } catch (Exception e) {
            log.error("定时大盘复盘失败: {}", e.getMessage(), e);
        }
    }
}
