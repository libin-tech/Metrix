package com.bin.stockanalysis.service;

import com.bin.stockanalysis.dto.request.NotificationConfigRequest;
import com.bin.stockanalysis.repository.entity.NotificationConfig;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    NotificationConfig createConfig(NotificationConfigRequest request);
    NotificationConfig updateConfig(Long id, NotificationConfigRequest request);
    NotificationConfig getConfigById(Long id);
    List<NotificationConfig> getAllConfigs();
    List<NotificationConfig> getActiveConfigs();
    void deleteConfig(Long id);
    boolean sendFeishuNotification(String title, String content);
    boolean sendFeishuNotification(String title, String content, Map<String, Object> attachments);
}
