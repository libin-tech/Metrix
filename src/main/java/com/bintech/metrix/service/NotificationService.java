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

    /**
     * 发送飞书卡片消息（个股分析）
     *
     * @param stockName 股票名称
     * @param stockCode 股票代码
     * @param coreInsight 核心洞察
     * @param overviewJson 概览JSON字符串
     * @param analysisTime 分析时间
     * @return 是否发送成功
     */
    boolean sendFeishuCardMessage(String stockName, String stockCode, String coreInsight, String overviewJson, String analysisTime);

    /**
     * 发送飞书卡片消息（大盘复盘）
     *
     * @param reviewName   复盘名称
     * @param reviewTime   复盘时间
     * @param summary      市场总结
     * @param avgChangePct 平均涨跌幅
     * @param coreSummary  核心总结
     * @return 是否发送成功
     */
    boolean sendFeishuMarketReviewCard(String reviewName, String reviewTime, String summary, double avgChangePct, String coreSummary);
}
