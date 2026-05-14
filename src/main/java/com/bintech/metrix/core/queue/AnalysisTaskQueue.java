package com.bintech.metrix.core.queue;

import com.bintech.metrix.enums.StockAnalysisStatus;
import com.bintech.metrix.repository.entity.StockAnalysisRecord;
import com.bintech.metrix.repository.mapper.StockAnalysisRecordMapper;
import com.bintech.metrix.service.StockAnalysisService;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分析任务队列 — 自研轻量级消息队列
 *
 * <p>基于 {@link BlockingQueue} 实现，启动固定数量的 daemon worker 线程消费任务。
 * 支持并发控制（最多3个任务同时执行）、队列饱和拒绝、失败状态自动回写。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisTaskQueue {

    /** 最大并发执行任务数 */
    private static final int MAX_CONCURRENT = 3;
    /** 排队队列容量 */
    private static final int QUEUE_CAPACITY = 10;

    private final BlockingQueue<AnalysisTask> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicInteger runningTasks = new AtomicInteger(0);

    private final StockAnalysisService stockAnalysisService;
    private final StockAnalysisRecordMapper recordMapper;

    private final List<Thread> workers = new ArrayList<>();
    private volatile boolean running = true;

    /**
     * 初始化 worker 线程池，启动队列消费
     */
    @PostConstruct
    public void init() {
        // 启动固定数量的daemon worker线程，应用退出时自动终止
        for (int i = 0; i < MAX_CONCURRENT; i++) {
            Thread worker = new Thread(this::loop, "analysis-worker-" + i);
            worker.setDaemon(true);
            worker.start();
            workers.add(worker);
        }
        log.info("分析任务队列已启动，worker线程数={}，队列容量={}", MAX_CONCURRENT, QUEUE_CAPACITY);
    }

    /**
     * 优雅关闭，中断所有 worker 线程
     */
    @PreDestroy
    public void destroy() {
        running = false;
        workers.forEach(Thread::interrupt);
        log.info("分析任务队列已关闭");
    }

    /**
     * 提交分析任务到队列
     *
     * @param task 分析任务
     * @return true-入队成功；false-队列已满拒绝
     */
    public boolean submit(AnalysisTask task) {
        boolean offered = queue.offer(task);
        if (offered) {
            log.info("任务已入队: recordId={}, stockName={}, 待处理={}, 运行中={}",
                    task.getRecordId(), task.getStockName(), queue.size(), runningTasks.get());
        } else {
            log.warn("队列已满，任务入队失败: recordId={}", task.getRecordId());
        }
        return offered;
    }

    /**
     * worker 主循环：阻塞式拉取任务并执行
     */
    private void loop() {
        // 不断从阻塞队列中拉取任务，空等3秒后继续轮询
        while (running) {
            try {
                AnalysisTask task = queue.poll(3, TimeUnit.SECONDS);
                if (task == null) continue;

                // 取出任务后立即增加运行计数，防止超发
                runningTasks.incrementAndGet();
                process(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * 执行单个分析任务，执行失败自动更新记录状态为 FAILED
     *
     * @param task 分析任务
     */
    private void process(AnalysisTask task) {
        long start = System.currentTimeMillis();
        try {
            log.info("开始执行分析任务: recordId={}, stockName={}", task.getRecordId(), task.getStockName());
            // 委托给StockAnalysisService执行实际分析逻辑
            stockAnalysisService.executeAnalysis(task.getRecordId(), task.getRequest());
            long elapsed = System.currentTimeMillis() - start;
            log.info("分析任务完成: recordId={}, stockName={}, 耗时={}ms", task.getRecordId(), task.getStockName(), elapsed);
        } catch (Exception e) {
            // 任何异常都将记录状态更新为失败
            log.error("分析任务失败: recordId={}, stockName={}, error={}", task.getRecordId(), task.getStockName(), e.getMessage(), e);
            try {
                StockAnalysisRecord record = new StockAnalysisRecord();
                record.setId(task.getRecordId());
                record.setStatus(StockAnalysisStatus.FAILED);
                recordMapper.updateById(record);
            } catch (Exception ex) {
                log.error("更新任务失败状态失败: recordId={}", task.getRecordId(), ex);
            }
        } finally {
            // 释放槽位，允许后续任务被执行
            runningTasks.decrementAndGet();
        }
    }

    /** 当前正在执行的任务数 */
    public int getRunningTaskCount() { return runningTasks.get(); }
    /** 排队等待的任务数 */
    public int getPendingTaskCount() { return queue.size(); }
    /** 最大并发数 */
    public int getMaxConcurrentTasks() { return MAX_CONCURRENT; }
    /** 队列是否可接受新任务 */
    public boolean canSubmit() { return queue.remainingCapacity() > 0; }
}
