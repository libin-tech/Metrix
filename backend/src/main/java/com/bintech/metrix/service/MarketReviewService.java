package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.CursorPageResult;
import com.bintech.metrix.repository.entity.MarketReview;

import java.util.List;
import java.util.Map;

/**
 * 大盘复盘服务接口
 */
public interface MarketReviewService {

    List<MarketReview> getAllReviews();

    CursorPageResult<MarketReview> cursorQuery(Long cursor, int limit);

    MarketReview getReviewById(Long id);

    void deleteReview(Long id);

    Map<String, Object> triggerReview();

    MarketReview createReview(String reviewDate);

    void processReview(Long reviewId, String reviewDate);
}
