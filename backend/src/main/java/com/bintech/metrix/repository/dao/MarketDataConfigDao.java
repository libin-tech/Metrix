package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.MarketDataConfig;

import java.util.List;

public interface MarketDataConfigDao {
    int insert(MarketDataConfig entity);
    int updateById(MarketDataConfig entity);
    int deleteById(Long id);
    int deactivateByUserId(Long userId);
    int deactivateByUserIdAndExcludeId(Long userId, Long excludeId);
    MarketDataConfig selectByIdAndUserId(Long id, Long userId);
    List<MarketDataConfig> selectByUserId(Long userId);
    List<MarketDataConfig> selectActiveByUserId(Long userId);
    long countByUserId(Long userId);
}
