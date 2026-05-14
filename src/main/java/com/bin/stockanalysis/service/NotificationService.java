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

    /**
     * 发送飞书卡片消息
     *
     * @param stockName 股票名称
     * @param stockCode 股票代码
     * @param coreInsight 核心洞察
     * @param overviewJson 概览JSON字符串
     * @param analysisTime 分析时间
     * @return 是否发送成功
     */
    boolean sendFeishuCardMessage(String stockName, String stockCode, String coreInsight, String overviewJson, String analysisTime);
}
