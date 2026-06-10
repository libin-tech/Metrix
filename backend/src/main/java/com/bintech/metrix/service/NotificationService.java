package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.NotificationConfigRequest;
import com.bintech.metrix.repository.entity.NotificationConfig;

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

    boolean sendFeishuNotification(String title, String content, Map<String, Object> attachments, Long userId);

    boolean sendFeishuCardMessage(String stockName, String stockCode, String coreInsight, String overviewJson, String analysisTime);

    boolean sendFeishuCardMessage(String stockName, String stockCode, String coreInsight, String overviewJson, String analysisTime, Long userId);

    boolean sendFeishuMarketReviewCard(String reviewName, String reviewTime, String summary, double avgChangePct, String coreSummary);

    boolean sendFeishuMarketReviewCard(String reviewName, String reviewTime, String summary, double avgChangePct, String coreSummary, Long userId);
}
