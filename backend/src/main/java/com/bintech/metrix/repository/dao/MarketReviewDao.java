package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.MarketReview;

import java.util.List;

public interface MarketReviewDao {
    int insert(MarketReview entity);
    int updateById(MarketReview entity);
    int deleteById(Long id);
    int deleteByUserIdAndIdNotIn(Long userId, List<Long> keepIds);
    MarketReview selectById(Long id);
    MarketReview selectByIdAndUserId(Long id, Long userId);
    MarketReview selectByReviewDateAndUserId(String reviewDate, Long userId);
    List<MarketReview> selectByUserIdOrderByReviewDateDesc(Long userId);
    List<MarketReview> cursorQueryByUserId(Long userId, Long cursor, int limit);
    List<MarketReview> selectTopByUserIdOrderByReviewDateDesc(Long userId, int limit);
    long countByUserId(Long userId);
}
