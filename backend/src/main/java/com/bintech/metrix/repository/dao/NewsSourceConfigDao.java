package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.NewsSourceConfig;

import java.util.List;

public interface NewsSourceConfigDao {
    int insert(NewsSourceConfig entity);
    int updateById(NewsSourceConfig entity);
    int deleteById(Long id);
    int deactivateByUserId(Long userId);
    int deactivateByUserIdAndExcludeId(Long userId, Long excludeId);
    NewsSourceConfig selectByIdAndUserId(Long id, Long userId);
    List<NewsSourceConfig> selectByUserId(Long userId);
    List<NewsSourceConfig> selectActiveByUserId(Long userId);
    long countByIdAndUserId(Long id, Long userId);
    long countActiveByUserId(Long userId);
}
