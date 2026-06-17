package com.bintech.metrix.repository.dao;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.repository.entity.AiModelConfig;
import com.bintech.metrix.repository.mapper.AiModelConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class AiModelConfigDaoImpl implements AiModelConfigDao {

    private final AiModelConfigMapper baseMapper;

    @Override
    public int insert(AiModelConfig entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(AiModelConfig entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public AiModelConfig selectByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            log.warn("selectByIdAndUserId: id or userId is null");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getId, id)
                .eq(AiModelConfig::getUserId, userId));
    }

    @Override
    public List<AiModelConfig> selectByUserId(Long userId) {
        if (userId == null) {
            log.warn("selectByUserId: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getUserId, userId)
                .orderByDesc(AiModelConfig::getCreateTime));
    }

    @Override
    public List<AiModelConfig> selectByUserIdAndModelType(Long userId, String modelType) {
        if (userId == null) {
            log.warn("selectByUserIdAndModelType: userId is null");
            return List.of();
        }
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getUserId, userId);
        if (StrUtil.isNotBlank(modelType)) {
            wrapper.eq(AiModelConfig::getModelType, modelType);
        }
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<AiModelConfig> selectActiveByUserId(Long userId) {
        if (userId == null) {
            log.warn("selectActiveByUserId: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getIsActive, true)
                .eq(AiModelConfig::getUserId, userId));
    }

    @Override
    public List<AiModelConfig> selectActiveByUserIdAndModelType(Long userId, String modelType) {
        if (userId == null) {
            log.warn("selectActiveByUserIdAndModelType: userId is null");
            return List.of();
        }
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getIsActive, true)
                .eq(AiModelConfig::getUserId, userId);
        if (StrUtil.isNotBlank(modelType)) {
            wrapper.eq(AiModelConfig::getModelType, modelType);
        }
        return baseMapper.selectList(wrapper);
    }

    @Override
    public AiModelConfig selectOneActiveByUserIdAndModelType(Long userId, String modelType) {
        if (userId == null || StrUtil.isBlank(modelType)) {
            log.warn("selectOneActiveByUserIdAndModelType: params invalid");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getIsActive, true)
                .eq(AiModelConfig::getUserId, userId)
                .eq(AiModelConfig::getModelType, modelType));
    }

    @Override
    public AiModelConfig selectOneActiveByModelType(String modelType) {
        if (StrUtil.isBlank(modelType)) {
            log.warn("selectOneActiveByModelType: modelType is blank");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getIsActive, true)
                .eq(AiModelConfig::getModelType, modelType));
    }

    @Override
    public long countByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            log.warn("countByIdAndUserId: id or userId is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getId, id)
                .eq(AiModelConfig::getUserId, userId));
    }
}
