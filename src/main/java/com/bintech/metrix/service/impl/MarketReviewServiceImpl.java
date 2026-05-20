package com.bintech.metrix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.core.analysis.MarketReviewDataFetcher;
import com.bintech.metrix.core.analysis.MarketReviewPromptBuilder;
import com.bintech.metrix.core.queue.MarketReviewTask;
import com.bintech.metrix.core.queue.MarketReviewTaskQueue;
import com.bintech.metrix.enums.MarketReviewStatus;
import com.bintech.metrix.repository.entity.MarketReview;
import com.bintech.metrix.repository.mapper.MarketReviewMapper;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.MarketReviewService;
import com.bintech.metrix.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketReviewServiceImpl implements MarketReviewService {

    private static final int MAX_RECORD_KEEP_COUNT = 100;
    private static final String SUMMARY_SLIGHT_UP = "小幅上涨";
    private static final String SUMMARY_BIG_UP = "大幅上涨";
    private static final String SUMMARY_SLIGHT_DOWN = "小幅下跌";
    private static final String SUMMARY_BIG_DOWN = "大幅下跌";
    private static final double SUMMARY_THRESHOLD = 3.0;
    private static final String CORE_SUMMARY_PREFIX = "【核心总结】";
    private static final int CORE_SUMMARY_MAX_LENGTH = 500;

    private final MarketReviewMapper marketReviewMapper;
    private final AiModelService aiModelService;
    private final NotificationService notificationService;
    private final MarketReviewDataFetcher marketReviewDataFetcher;
    private final MarketReviewPromptBuilder marketReviewPromptBuilder;

    @Autowired
    @Lazy
    private MarketReviewTaskQueue marketReviewTaskQueue;

    @Override
    public List<MarketReview> getAllReviews() {
        LambdaQueryWrapper<MarketReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(MarketReview::getReviewDate);
        return marketReviewMapper.selectList(wrapper);
    }

    @Override
    public MarketReview getReviewById(Long id) {
        MarketReview review = marketReviewMapper.selectById(id);
        if (review == null) {
            throw new RuntimeException("复盘记录不存在");
        }
        return review;
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        MarketReview review = marketReviewMapper.selectById(id);
        if (review == null) {
            throw new RuntimeException("复盘记录不存在");
        }
        marketReviewMapper.deleteById(id);
        log.info("大盘复盘记录删除成功，ID: {}", id);
    }

    @Override
    public Map<String, Object> triggerReview() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        boolean isBeforeClose = !isWeekend && now.isBefore(LocalTime.of(15, 0));

        LocalDate targetDate;
        if (isWeekend || isBeforeClose) {
            targetDate = getPreviousTradingDay(today);
        } else {
            targetDate = today;
        }

        String dateStr = targetDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String reviewName = dateStr + "A股复盘报告";

        MarketReview existing = marketReviewMapper.selectOne(
                new LambdaQueryWrapper<MarketReview>()
                        .eq(MarketReview::getReviewDate, dateStr));

        Map<String, Object> result = new HashMap<>();
        result.put("action", isBeforeClose && !isWeekend ? "NOT_CLOSED" : "REVIEW_TODAY");
        result.put("targetDate", dateStr);
        result.put("reviewName", reviewName);
        result.put("existingReview", existing);
        result.put("notClosed", isWeekend || isBeforeClose);
        return result;
    }

    @Override
    @Transactional
    public MarketReview createReview(String reviewDate) {
        log.info("创建大盘复盘记录: reviewDate={}", reviewDate);

        MarketReview existing = marketReviewMapper.selectOne(
                new LambdaQueryWrapper<MarketReview>()
                        .eq(MarketReview::getReviewDate, reviewDate));

        if (existing != null) {
            marketReviewMapper.deleteById(existing.getId());
        }

        String reviewName = reviewDate + " A股复盘报告";
        LocalDateTime now = LocalDateTime.now();

        MarketReview review = new MarketReview();
        review.setReviewDate(reviewDate);
        review.setReviewName(reviewName);
        review.setReviewTime(now);
        review.setStatus(MarketReviewStatus.REVIEWING);
        review.setCreateTime(now);
        review.setUpdateTime(now);
        marketReviewMapper.insert(review);

        cleanupExcessRecords();

        MarketReviewTask task = new MarketReviewTask(review.getId(), reviewDate);
        boolean submitted = marketReviewTaskQueue.submit(task);
        if (!submitted) {
            review.setStatus(MarketReviewStatus.FAILED);
            review.setErrorMessage("复盘任务队列已满，请稍后重试");
            marketReviewMapper.updateById(review);
        }

        log.info("大盘复盘记录已创建并提交到队列: id={}, reviewDate={}", review.getId(), reviewDate);
        return review;
    }

    @Override
    @Transactional
    public void processReview(Long reviewId, String reviewDate) {
        log.info("开始执行大盘复盘: reviewId={}, reviewDate={}", reviewId, reviewDate);

        Map<String, Object> indexData;
        try {
            indexData = marketReviewDataFetcher.fetchIndexData(reviewDate);
            if (indexData.isEmpty()) {
                throw new RuntimeException("获取指数数据失败");
            }
        } catch (Exception e) {
            failReview(reviewId, "获取指数数据失败: " + e.getMessage());
            return;
        }

        String prompt = marketReviewPromptBuilder.build(indexData, reviewDate);
        String modelType = aiModelService.getActiveModelType();

        String content;
        try {
            content = aiModelService.generateAnalysis(prompt, modelType);
        } catch (Exception e) {
            failReview(reviewId, "AI分析失败: " + e.getMessage());
            return;
        }

        double avgChangePct = calcAvgChangePct(indexData);
        String summary = calcSummary(avgChangePct);
        String coreSummary = extractCoreSummary(content);
        LocalDateTime now = LocalDateTime.now();

        MarketReview review = marketReviewMapper.selectById(reviewId);
        if (review == null) {
            log.warn("大盘复盘记录不存在，无法更新: id={}", reviewId);
            return;
        }
        review.setReviewTime(now);
        review.setDetail(content);
        review.setSummary(summary);
        review.setCoreSummary(coreSummary);
        review.setStatus(MarketReviewStatus.COMPLETED);
        review.setUpdateTime(now);
        marketReviewMapper.updateById(review);

        try {
            sendFeishuReview(review, avgChangePct);
        } catch (Exception e) {
            log.warn("大盘复盘飞书推送失败: {}", e.getMessage());
        }

        log.info("大盘复盘执行完成: reviewId={}, reviewDate={}, summary={}", reviewId, reviewDate, summary);
    }

    private LocalDate getPreviousTradingDay(LocalDate date) {
        LocalDate prev = date.minusDays(1);
        while (prev.getDayOfWeek() == DayOfWeek.SATURDAY || prev.getDayOfWeek() == DayOfWeek.SUNDAY) {
            prev = prev.minusDays(1);
        }
        return prev;
    }

    private void failReview(Long reviewId, String errorMessage) {
        try {
            MarketReview review = new MarketReview();
            review.setId(reviewId);
            review.setStatus(MarketReviewStatus.FAILED);
            review.setErrorMessage(errorMessage);
            marketReviewMapper.updateById(review);
            log.error("大盘复盘失败: reviewId={}, error={}", reviewId, errorMessage);
        } catch (Exception e) {
            log.error("更新大盘复盘失败状态异常: reviewId={}", reviewId, e);
        }
    }

    private double calcAvgChangePct(Map<String, Object> indexData) {
        double sum = 0;
        int count = 0;
        for (Map.Entry<String, Object> entry : indexData.entrySet()) {
            Map<String, Object> idx = (Map<String, Object>) entry.getValue();
            Object pct = idx.get("changePct");
            if (pct instanceof Number) {
                sum += ((Number) pct).doubleValue();
                count++;
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    private String calcSummary(double avgChangePct) {
        if (avgChangePct >= SUMMARY_THRESHOLD) return SUMMARY_BIG_UP;
        if (avgChangePct > 0) return SUMMARY_SLIGHT_UP;
        if (avgChangePct > -SUMMARY_THRESHOLD) return SUMMARY_SLIGHT_DOWN;
        return SUMMARY_BIG_DOWN;
    }

    private void cleanupExcessRecords() {
        long total = marketReviewMapper.selectCount(new LambdaQueryWrapper<>());
        if (total <= MAX_RECORD_KEEP_COUNT) return;

        List<MarketReview> latestRecords = marketReviewMapper.selectList(
                new LambdaQueryWrapper<MarketReview>()
                        .orderByDesc(MarketReview::getReviewDate)
                        .last("LIMIT " + MAX_RECORD_KEEP_COUNT));
        List<Long> keepIds = latestRecords.stream()
                .map(MarketReview::getId)
                .toList();
        marketReviewMapper.delete(new LambdaQueryWrapper<MarketReview>()
                .notIn(MarketReview::getId, keepIds));
    }

    private String extractCoreSummary(String detail) {
        if (detail == null || detail.isBlank()) {
            return "";
        }
        int idx = detail.indexOf(CORE_SUMMARY_PREFIX);
        if (idx != -1) {
            String core = detail.substring(idx + CORE_SUMMARY_PREFIX.length()).trim();
            if (core.length() > CORE_SUMMARY_MAX_LENGTH) {
                core = core.substring(0, CORE_SUMMARY_MAX_LENGTH);
            }
            return core;
        }
        return detail.substring(0, Math.min(detail.length(), CORE_SUMMARY_MAX_LENGTH));
    }

    private void sendFeishuReview(MarketReview review, double avgChangePct) {
        String coreSummary = review.getCoreSummary();
        if (coreSummary == null || coreSummary.isBlank()) {
            coreSummary = "暂无核心总结";
        }
        String reviewTime = review.getReviewTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        notificationService.sendFeishuMarketReviewCard(
                review.getReviewName(), reviewTime, review.getSummary(), avgChangePct, coreSummary);
    }
}
