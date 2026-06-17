package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.AiModelConfig;

import java.util.List;

public interface AiModelConfigDao {
    int insert(AiModelConfig entity);
    int updateById(AiModelConfig entity);
    int deleteById(Long id);
    AiModelConfig selectByIdAndUserId(Long id, Long userId);
    List<AiModelConfig> selectByUserId(Long userId);
    List<AiModelConfig> selectByUserIdAndModelType(Long userId, String modelType);
    List<AiModelConfig> selectActiveByUserId(Long userId);
    List<AiModelConfig> selectActiveByUserIdAndModelType(Long userId, String modelType);
    AiModelConfig selectOneActiveByUserIdAndModelType(Long userId, String modelType);
    AiModelConfig selectOneActiveByModelType(String modelType);
    long countByIdAndUserId(Long id, Long userId);
}
