package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.NotificationConfig;

import java.util.List;

public interface NotificationConfigDao {
    int insert(NotificationConfig entity);
    int updateById(NotificationConfig entity);
    int deleteById(Long id);
    int deactivateByUserId(Long userId);
    int deactivateByUserIdAndExcludeId(Long userId, Long excludeId);
    NotificationConfig selectByIdAndUserId(Long id, Long userId);
    List<NotificationConfig> selectByUserId(Long userId);
    List<NotificationConfig> selectActiveByUserId(Long userId);
    NotificationConfig selectOneActiveByUserIdAndNotifyType(Long userId, String notifyType);
    long countByIdAndUserId(Long id, Long userId);
    long countActiveByUserId(Long userId);
}
