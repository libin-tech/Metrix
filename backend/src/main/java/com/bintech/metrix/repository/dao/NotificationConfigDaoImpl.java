package com.bintech.metrix.repository.dao;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bintech.metrix.repository.entity.NotificationConfig;
import com.bintech.metrix.repository.mapper.NotificationConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class NotificationConfigDaoImpl implements NotificationConfigDao {

    private final NotificationConfigMapper baseMapper;

    @Override
    public int insert(NotificationConfig entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(NotificationConfig entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public int deactivateByUserId(Long userId) {
        if (userId == null) {
            log.warn("deactivateByUserId: userId is null");
            return 0;
        }
        return baseMapper.update(null, new LambdaUpdateWrapper<NotificationConfig>()
                .set(NotificationConfig::getIsActive, false)
                .eq(NotificationConfig::getUserId, userId));
    }

    @Override
    public int deactivateByUserIdAndExcludeId(Long userId, Long excludeId) {
        if (userId == null || excludeId == null) {
            log.warn("deactivateByUserIdAndExcludeId: userId or excludeId is null");
            return 0;
        }
        return baseMapper.update(null, new LambdaUpdateWrapper<NotificationConfig>()
                .set(NotificationConfig::getIsActive, false)
                .ne(NotificationConfig::getId, excludeId)
                .eq(NotificationConfig::getUserId, userId));
    }

    @Override
    public NotificationConfig selectByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            log.warn("selectByIdAndUserId: id or userId is null");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<NotificationConfig>()
                .eq(NotificationConfig::getId, id)
                .eq(NotificationConfig::getUserId, userId));
    }

    @Override
    public List<NotificationConfig> selectByUserId(Long userId) {
        if (userId == null) {
            log.warn("selectByUserId: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<NotificationConfig>()
                .eq(NotificationConfig::getUserId, userId));
    }

    @Override
    public List<NotificationConfig> selectActiveByUserId(Long userId) {
        if (userId == null) {
            log.warn("selectActiveByUserId: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<NotificationConfig>()
                .eq(NotificationConfig::getIsActive, true)
                .eq(NotificationConfig::getUserId, userId));
    }

    @Override
    public NotificationConfig selectOneActiveByUserIdAndNotifyType(Long userId, String notifyType) {
        if (userId == null || StrUtil.isBlank(notifyType)) {
            log.warn("selectOneActiveByUserIdAndNotifyType: params invalid");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<NotificationConfig>()
                .eq(NotificationConfig::getIsActive, true)
                .eq(NotificationConfig::getUserId, userId)
                .eq(NotificationConfig::getChannelType, notifyType));
    }

    @Override
    public long countByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            log.warn("countByIdAndUserId: id or userId is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<NotificationConfig>()
                .eq(NotificationConfig::getId, id)
                .eq(NotificationConfig::getUserId, userId));
    }

    @Override
    public long countActiveByUserId(Long userId) {
        if (userId == null) {
            log.warn("countActiveByUserId: userId is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<NotificationConfig>()
                .eq(NotificationConfig::getIsActive, true)
                .eq(NotificationConfig::getUserId, userId));
    }
}
