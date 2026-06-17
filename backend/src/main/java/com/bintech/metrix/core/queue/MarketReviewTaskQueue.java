package com.bintech.metrix.core.queue;

import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.enums.MarketReviewStatus;
import com.bintech.metrix.repository.dao.MarketReviewDao;
import com.bintech.metrix.repository.entity.MarketReview;
import com.bintech.metrix.service.MarketReviewService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 大盘复盘任务队列 — 自研轻量级消息队列
 * <p>基于 {@link BlockingQueue} 实现，启动2个虚拟线程消费复盘任务，
 * 支持队列饱和拒绝和失败状态自动回写。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketReviewTaskQueue {

    private static final int MAX_CONCURRENT = 2;
    private static final int QUEUE_CAPACITY = 10;

    private final BlockingQueue<MarketReviewTask> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private final MarketReviewService marketReviewService;
    private final MarketReviewDao marketReviewDao;

    private final List<Thread> workers = new ArrayList<>();
    private volatile boolean running = true;

    @PostConstruct
    public void init() {
        for (int i = 0; i < MAX_CONCURRENT; i++) {
            Thread worker = Thread.ofVirtual()
                    .name("market-review-worker-" + i)
                    .start(this::loop);
            workers.add(worker);
        }
        log.info("大盘复盘任务队列已启动，worker线程数={}，队列容量={}", MAX_CONCURRENT, QUEUE_CAPACITY);
    }

    @PreDestroy
    public void destroy() {
        running = false;
        workers.forEach(Thread::interrupt);
        log.info("大盘复盘任务队列已关闭");
    }

    /**
     * 提交复盘任务到队列，队列满时返回false
     */
    public boolean submit(MarketReviewTask task) {
        boolean offered = queue.offer(task);
        if (offered) {
            log.info("大盘复盘任务已入队: reviewId={}, reviewDate={}, 待处理={}",
                    task.getReviewId(), task.getReviewDate(), queue.size());
        } else {
            log.warn("大盘复盘队列已满，任务入队失败: reviewId={}", task.getReviewId());
        }
        return offered;
    }

    private void loop() {
        while (running) {
            try {
                MarketReviewTask task = queue.poll(SystemConstants.POLL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (task == null) continue;
                process(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void process(MarketReviewTask task) {
        long start = System.currentTimeMillis();
        try {
            log.info("开始执行大盘复盘任务: reviewId={}, reviewDate={}", task.getReviewId(), task.getReviewDate());
            marketReviewService.processReview(task.getReviewId(), task.getReviewDate(), task.getUserId());
            long elapsed = System.currentTimeMillis() - start;
            log.info("大盘复盘任务完成: reviewId={}, reviewDate={}, 耗时={}ms", task.getReviewId(), task.getReviewDate(), elapsed);
        } catch (Exception e) {
            log.error("大盘复盘任务失败: reviewId={}, reviewDate={}, error={}", task.getReviewId(), task.getReviewDate(), e.getMessage(), e);
            try {
                MarketReview record = new MarketReview();
                record.setId(task.getReviewId());
                record.setStatus(MarketReviewStatus.FAILED);
                record.setErrorMessage(e.getMessage());
                marketReviewDao.updateById(record);
            } catch (Exception ex) {
                log.error("更新大盘复盘失败状态失败: reviewId={}", task.getReviewId(), ex);
            }
        }
    }
}
