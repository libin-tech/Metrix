package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.core.analysis.MarketReviewDataFetcher;
import com.bintech.metrix.core.analysis.MarketReviewPromptBuilder;
import com.bintech.metrix.core.queue.MarketReviewTask;
import com.bintech.metrix.core.queue.MarketReviewTaskQueue;
import com.bintech.metrix.dto.response.CursorPageResult;
import com.bintech.metrix.enums.MarketReviewStatus;
import com.bintech.metrix.repository.dao.MarketReviewDao;
import com.bintech.metrix.repository.entity.MarketReview;
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
    private static final String SUMMARY_MICRO_UP = "微涨";
    private static final String SUMMARY_MICRO_DOWN = "微跌";
    private static final String SUMMARY_SMALL_UP = "小涨";
    private static final String SUMMARY_SMALL_DOWN = "小跌";
    private static final String SUMMARY_LARGE_UP = "大涨";
    private static final String SUMMARY_LARGE_DOWN = "大跌";
    private static final String SUMMARY_SURGE_UP = "暴涨";
    private static final String SUMMARY_SURGE_DOWN = "暴跌";
    private static final double THRESHOLD_MICRO = 0.5;
    private static final double THRESHOLD_SMALL = 1.5;
    private static final double THRESHOLD_LARGE = 3.5;
    private static final String CORE_SUMMARY_PREFIX = "【核心总结】";
    private static final int CORE_SUMMARY_MAX_LENGTH = 500;

    private final MarketReviewDao marketReviewDao;
    private final AiModelService aiModelService;
    private final NotificationService notificationService;
    private final MarketReviewDataFetcher marketReviewDataFetcher;
    private final MarketReviewPromptBuilder marketReviewPromptBuilder;

    @Autowired
    @Lazy
    private MarketReviewTaskQueue marketReviewTaskQueue;

    /**
     * 获取当前用户的所有大盘复盘记录，按复盘日期倒序
     */
    @Override
    public List<MarketReview> getAllReviews() {
        Long userId = StpUtil.getLoginIdAsLong();
        return marketReviewDao.selectByUserIdOrderByReviewDateDesc(userId);
    }

    /**
     * 游标分页查询当前用户的复盘记录
     */
    @Override
    public CursorPageResult<MarketReview> cursorQuery(Long cursor, int limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<MarketReview> records = marketReviewDao.cursorQueryByUserId(userId, cursor, limit + 1);
        boolean hasMore = records.size() > limit;
        if (hasMore) {
            records = records.subList(0, limit);
        }
        Long nextCursor = records.isEmpty() ? null : records.getLast().getId();
        return CursorPageResult.<MarketReview>builder()
                .items(records)
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .build();
    }

    /**
     * 查询单条复盘记录（含用户隔离校验）
     */
    @Override
    public MarketReview getReviewById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        MarketReview review = marketReviewDao.selectByIdAndUserId(id, userId);
        if (review == null) {
            throw new RuntimeException("复盘记录不存在");
        }
        return review;
    }

    @Override
    @Transactional
    public void deleteReview(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        MarketReview review = marketReviewDao.selectByIdAndUserId(id, userId);
        if (review == null) {
            throw new RuntimeException("复盘记录不存在");
        }
        marketReviewDao.deleteById(id);
        log.info("大盘复盘记录删除成功，ID: {}", id);
    }

    /**
     * 触发大盘复盘：根据当前时间判断目标交易日，
     * 周末或15:00前取前一个交易日，否则取今日
     */
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

        Map<String, Object> result = new HashMap<>();
        result.put("action", isBeforeClose && !isWeekend ? "NOT_CLOSED" : "REVIEW_TODAY");
        result.put("targetDate", dateStr);
        result.put("reviewName", reviewName);
        result.put("notClosed", isWeekend || isBeforeClose);
        return result;
    }

    /**
     * 创建大盘复盘记录并提交到任务队列异步执行
     * 若已存在同日期记录则先删除再创建
     */
    @Override
    @Transactional
    public MarketReview createReview(String reviewDate) {
        Long userId = StpUtil.getLoginIdAsLong();
        return doCreateReview(reviewDate, userId);
    }

    @Override
    @Transactional
    public MarketReview createReview(String reviewDate, Long userId) {
        return doCreateReview(reviewDate, userId);
    }

    private MarketReview doCreateReview(String reviewDate, Long userId) {
        log.info("创建大盘复盘记录: reviewDate={}, userId={}", reviewDate, userId);

        MarketReview existing = marketReviewDao.selectByReviewDateAndUserId(reviewDate, userId);

        if (existing != null) {
            marketReviewDao.deleteById(existing.getId());
        }

        String reviewName = reviewDate + " A股复盘报告";
        LocalDateTime now = LocalDateTime.now();

        MarketReview review = new MarketReview();
        review.setReviewDate(reviewDate);
        review.setReviewName(reviewName);
        review.setReviewTime(now);
        review.setStatus(MarketReviewStatus.REVIEWING);
        review.setUserId(userId);
        review.setCreateTime(now);
        review.setUpdateTime(now);
        marketReviewDao.insert(review);

        cleanupExcessRecords(userId);

        MarketReviewTask task = new MarketReviewTask(review.getId(), reviewDate, userId);
        boolean submitted = marketReviewTaskQueue.submit(task);
        if (!submitted) {
            review.setStatus(MarketReviewStatus.FAILED);
            review.setErrorMessage("复盘任务队列已满，请稍后重试");
            marketReviewDao.updateById(review);
        }

        log.info("大盘复盘记录已创建并提交到队列: id={}, reviewDate={}", review.getId(), reviewDate);
        return review;
    }

    @Override
    @Transactional
    public void processReview(Long reviewId, String reviewDate) {
        processReview(reviewId, reviewDate, null);
    }

    /**
     * 执行大盘复盘核心流程：
     * 1. 获取四大指数行情和K线数据
     * 2. 构建提示词调用AI生成复盘报告
     * 3. 计算涨跌幅均值并生成摘要标签
     * 4. 提取核心总结并推送飞书
     */
    @Override
    @Transactional
    public void processReview(Long reviewId, String reviewDate, Long userId) {
        log.info("开始执行大盘复盘: reviewId={}, reviewDate={}, userId={}", reviewId, reviewDate, userId);

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
        String modelType = aiModelService.getActiveModelType(userId);

        String content;
        try {
            content = aiModelService.generateAnalysis(prompt, modelType, userId);
        } catch (Exception e) {
            failReview(reviewId, "AI分析失败: " + e.getMessage());
            return;
        }

        double avgChangePct = calcAvgChangePct(indexData);
        String summary = calcSummary(avgChangePct);
        String coreSummary = extractCoreSummary(content);
        LocalDateTime now = LocalDateTime.now();

        MarketReview review = marketReviewDao.selectById(reviewId);
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
        marketReviewDao.updateById(review);

        try {
            sendFeishuReview(review, avgChangePct, userId);
        } catch (Exception e) {
            log.warn("大盘复盘飞书推送失败: {}", e.getMessage());
        }

        log.info("大盘复盘执行完成: reviewId={}, reviewDate={}, summary={}", reviewId, reviewDate, summary);
    }

    /**
     * 获取前一个交易日（跳过周末）
     */
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
            marketReviewDao.updateById(review);
            log.error("大盘复盘失败: reviewId={}, error={}", reviewId, errorMessage);
        } catch (Exception e) {
            log.error("更新大盘复盘失败状态异常: reviewId={}", reviewId, e);
        }
    }

    /**
     * 计算四大指数的平均涨跌幅，用于生成概要标签
     */
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

    /**
     * 根据涨跌幅映射为文本标签：微涨/微跌/小涨/小跌/大涨/大跌/暴涨/暴跌
     */
    private String calcSummary(double avgChangePct) {
        double abs = Math.abs(avgChangePct);
        if (avgChangePct >= 0) {
            if (abs >= THRESHOLD_LARGE) return SUMMARY_SURGE_UP;
            if (abs >= THRESHOLD_SMALL) return SUMMARY_LARGE_UP;
            if (abs >= THRESHOLD_MICRO) return SUMMARY_SMALL_UP;
            return SUMMARY_MICRO_UP;
        } else {
            if (abs >= THRESHOLD_LARGE) return SUMMARY_SURGE_DOWN;
            if (abs >= THRESHOLD_SMALL) return SUMMARY_LARGE_DOWN;
            if (abs >= THRESHOLD_MICRO) return SUMMARY_SMALL_DOWN;
            return SUMMARY_MICRO_DOWN;
        }
    }

    private void cleanupExcessRecords(Long userId) {
        long total = marketReviewDao.countByUserId(userId);
        if (total <= MAX_RECORD_KEEP_COUNT) return;

        List<MarketReview> latestRecords = marketReviewDao.selectTopByUserIdOrderByReviewDateDesc(userId, MAX_RECORD_KEEP_COUNT);
        List<Long> keepIds = latestRecords.stream()
                .map(MarketReview::getId)
                .toList();
        marketReviewDao.deleteByUserIdAndIdNotIn(userId, keepIds);
    }

    /**
     * 从AI生成的复盘中提取【核心总结】段落，限500字
     */
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

    private void sendFeishuReview(MarketReview review, double avgChangePct, Long userId) {
        String coreSummary = review.getCoreSummary();
        if (coreSummary == null || coreSummary.isBlank()) {
            coreSummary = "暂无核心总结";
        }
        String reviewTime = review.getReviewTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        notificationService.sendFeishuMarketReviewCard(
                review.getReviewName(), reviewTime, review.getSummary(), avgChangePct, coreSummary, userId);
    }
}
