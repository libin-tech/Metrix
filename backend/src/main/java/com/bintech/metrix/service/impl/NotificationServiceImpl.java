package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.dto.request.NotificationConfigRequest;
import com.bintech.metrix.repository.dao.NotificationConfigDao;
import com.bintech.metrix.repository.entity.NotificationConfig;
import com.bintech.metrix.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationConfigDao notificationConfigDao;

    @Override
    @Transactional
    public NotificationConfig createConfig(NotificationConfigRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (Boolean.TRUE.equals(request.getIsActive())) {
            notificationConfigDao.deactivateByUserId(userId);
        }

        NotificationConfig config = new NotificationConfig();
        config.setChannelType(request.getChannelType());
        config.setWebhookUrl(request.getWebhookUrl());
        config.setSecret(request.getSecret());
        config.setIsActive(request.getIsActive());
        config.setUserId(userId);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        notificationConfigDao.insert(config);
        return config;
    }

    @Override
    @Transactional
    public NotificationConfig updateConfig(Long id, NotificationConfigRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        NotificationConfig config = notificationConfigDao.selectByIdAndUserId(id, userId);
        if (config == null) {
            throw new RuntimeException("Notification config not found");
        }

        if (Boolean.TRUE.equals(request.getIsActive())) {
            notificationConfigDao.deactivateByUserIdAndExcludeId(userId, id);
        }

        config.setChannelType(request.getChannelType());
        config.setWebhookUrl(request.getWebhookUrl());
        config.setSecret(request.getSecret());
        config.setIsActive(request.getIsActive());
        config.setUpdateTime(LocalDateTime.now());
        notificationConfigDao.updateById(config);
        return config;
    }

    @Override
    public NotificationConfig getConfigById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        NotificationConfig config = notificationConfigDao.selectByIdAndUserId(id, userId);
        if (config == null) {
            throw new RuntimeException("Notification config not found");
        }
        return config;
    }

    @Override
    public List<NotificationConfig> getAllConfigs() {
        Long userId = StpUtil.getLoginIdAsLong();
        return notificationConfigDao.selectByUserId(userId);
    }

    @Override
    public List<NotificationConfig> getActiveConfigs() {
        Long userId = StpUtil.getLoginIdAsLong();
        return notificationConfigDao.selectActiveByUserId(userId);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        long count = notificationConfigDao.countByIdAndUserId(id, userId);
        if (count == 0) {
            throw new RuntimeException("Notification config not found");
        }
        notificationConfigDao.deleteById(id);
    }

    private NotificationConfig getActiveFeishuConfig() {
        Long userId = StpUtil.getLoginIdAsLong();
        return getActiveFeishuConfig(userId);
    }

    private NotificationConfig getActiveFeishuConfig(Long userId) {
        if (userId == null) {
            return null;
        }
        List<NotificationConfig> activeConfigs = notificationConfigDao.selectActiveByUserId(userId);
        NotificationConfig config = activeConfigs.stream()
                .filter(c -> BusinessConstants.CHANNEL_TYPE_FEISHU.equals(c.getChannelType()))
                .findFirst()
                .orElse(null);
        if (config == null) {
            log.warn("未找到活跃的飞书通知配置");
        }
        return config;
    }

    @Override
    public boolean sendFeishuNotification(String title, String content) {
        return sendFeishuNotification(title, content, null);
    }

    @Override
    public boolean sendFeishuNotification(String title, String content, Map<String, Object> attachments) {
        return sendFeishuNotification(title, content, attachments, null);
    }

    @Override
    public boolean sendFeishuNotification(String title, String content, Map<String, Object> attachments, Long userId) {
        NotificationConfig config = getActiveFeishuConfig(userId);
        if (config == null) {
            return false;
        }

        JSONObject message = new JSONObject();
        JSONObject textContent = new JSONObject();

        if (attachments != null && !attachments.isEmpty()) {
            textContent.put("title", title);
            textContent.put("text", content);
            textContent.put("attachments", attachments);
            message.put("msg_type", "post");
            message.put("content", textContent);
        } else {
            message.put("msg_type", "text");
            message.put("content", new JSONObject().put("text", title + "\n" + content));
        }

        return doPost(config.getWebhookUrl(), message);
    }

    @Override
    public boolean sendFeishuCardMessage(String stockName, String stockCode, String coreInsight, String overviewJson, String analysisTime) {
        Long userId = StpUtil.getLoginIdAsLong();
        return sendFeishuCardMessage(stockName, stockCode, coreInsight, overviewJson, analysisTime, userId);
    }

    @Override
    public boolean sendFeishuCardMessage(String stockName, String stockCode, String coreInsight, String overviewJson, String analysisTime, Long userId) {
        NotificationConfig config = getActiveFeishuConfig(userId);
        if (config == null) {
            log.warn("未找到活跃的飞书通知配置");
            return false;
        }

        JSONObject overview = JSONUtil.parseObj(overviewJson);
        JSONObject market = overview.getJSONObject("realTimeMarket");
        JSONObject pivot = overview.getJSONObject("dataPivot");
        JSONObject battlePlan = overview.getJSONObject("battlePlan");

        StringBuilder marketMd = new StringBuilder();
        if (market != null) {
            String price = market.getStr("currentPrice", "--");
            String changePct = market.getStr("changePercent", "0");
            String changeAmt = market.getStr("changeAmount", "0");
            String open = market.getStr("openPrice", "--");
            String high = market.getStr("highPrice", "--");
            String low = market.getStr("lowPrice", "--");
            String close = market.getStr("prevClosePrice", "--");
            String vol = market.getStr("volume", "0");
            String amt = market.getStr("turnover", "0");
            String turnoverRate = market.getStr("turnoverRate", "--");
            String volRatio = market.getStr("volumeRatio", "--");
            marketMd.append("** 实时行情**\n\n");
            marketMd.append("现价：¥").append(price).append("（").append(changePct).append("%）\n");
            marketMd.append("开盘：").append(open).append(" | 最高：").append(high).append(" | 最低：").append(low).append("\n");
            marketMd.append("昨收：").append(close).append("\n");
            marketMd.append("成交量：").append(vol).append(" | 成交额：").append(amt).append("\n");
            marketMd.append("换手率：").append(turnoverRate).append("% | 量比：").append(volRatio);
        }

        StringBuilder pivotMd = new StringBuilder();
        if (pivot != null) {
            pivotMd.append("** 关键指标**\n\n");
            pivotMd.append("MA5：").append(pivot.getStr("ma5", "--"));
            pivotMd.append(" | MA20：").append(pivot.getStr("ma20", "--"));
            pivotMd.append(" | MA60：").append(pivot.getStr("ma60", "--")).append("\n");
            pivotMd.append("支撑位：").append(pivot.getStr("supportLevel", "--"));
            pivotMd.append(" | 压力位：").append(pivot.getStr("resistanceLevel", "--")).append("\n");
            pivotMd.append("获利盘：").append(pivot.getStr("profitRatio", "--")).append("%");
            pivotMd.append(" | 套牢盘：").append(pivot.getStr("lossRatio", "--")).append("%");
            String avgCost = pivot.getStr("avgCostPrice");
            if (avgCost != null && !avgCost.isEmpty()) {
                pivotMd.append(" | 均价：¥").append(avgCost);
            }
        }

        StringBuilder planMd = new StringBuilder();
        if (battlePlan != null) {
            planMd.append("** 作战计划**\n\n");
            planMd.append("理想入场：¥").append(battlePlan.getStr("idealEntryPrice", "--")).append("\n");
            planMd.append("止损位：¥").append(battlePlan.getStr("stopLossPrice", "--")).append("\n");
            planMd.append("目标位：¥").append(battlePlan.getStr("targetPrice", "--")).append("\n");
            planMd.append("风险回报比：1:").append(battlePlan.getStr("riskRewardRatio", "--"));
        }

        JSONObject card = new JSONObject();
        card.put("config", new JSONObject().put("wide_screen_mode", true));

        JSONObject header = new JSONObject();
        header.put("template", "blue");
        JSONObject title = new JSONObject();
        title.put("tag", "plain_text");
        title.put("content", stockName + "（" + stockCode + "）分析报告");
        header.put("title", title);
        card.put("header", header);

        cn.hutool.json.JSONArray elements = new cn.hutool.json.JSONArray();

        JSONObject timeDiv = new JSONObject();
        timeDiv.put("tag", "div");
        JSONObject timeText = new JSONObject();
        timeText.put("tag", "lark_md");
        timeText.put("content", "**分析时间：** " + analysisTime);
        timeDiv.put("text", timeText);
        elements.add(timeDiv);

        elements.add(new JSONObject().put("tag", "hr"));

        if (coreInsight != null && !coreInsight.isEmpty()) {
            JSONObject insightDiv = new JSONObject();
            insightDiv.put("tag", "div");
            JSONObject insightText = new JSONObject();
            insightText.put("tag", "lark_md");
            insightText.put("content", "**核心洞察**\n\n" + coreInsight);
            insightDiv.put("text", insightText);
            elements.add(insightDiv);
            elements.add(new JSONObject().put("tag", "hr"));
        }

        if (marketMd.length() > 0) {
            JSONObject marketDiv = new JSONObject();
            marketDiv.put("tag", "div");
            JSONObject marketText = new JSONObject();
            marketText.put("tag", "lark_md");
            marketText.put("content", marketMd.toString());
            marketDiv.put("text", marketText);
            elements.add(marketDiv);
            elements.add(new JSONObject().put("tag", "hr"));
        }

        if (pivotMd.length() > 0) {
            JSONObject pivotDiv = new JSONObject();
            pivotDiv.put("tag", "div");
            JSONObject pivotText = new JSONObject();
            pivotText.put("tag", "lark_md");
            pivotText.put("content", pivotMd.toString());
            pivotDiv.put("text", pivotText);
            elements.add(pivotDiv);
            elements.add(new JSONObject().put("tag", "hr"));
        }

        if (planMd.length() > 0) {
            JSONObject planDiv = new JSONObject();
            planDiv.put("tag", "div");
            JSONObject planText = new JSONObject();
            planText.put("tag", "lark_md");
            planText.put("content", planMd.toString());
            planDiv.put("text", planText);
            elements.add(planDiv);
            elements.add(new JSONObject().put("tag", "hr"));
        }

        JSONObject note = new JSONObject();
        note.put("tag", "note");
        cn.hutool.json.JSONArray noteElements = new cn.hutool.json.JSONArray();
        JSONObject noteText = new JSONObject();
        noteText.put("tag", "plain_text");
        noteText.put("content", "详细分析报告及完整内容请登录系统查看");
        noteElements.add(noteText);
        note.put("elements", noteElements);
        elements.add(note);

        card.put("elements", elements);

        JSONObject message = new JSONObject();
        message.put("msg_type", "interactive");
        message.put("card", card);

        return doPost(config.getWebhookUrl(), message);
    }

    @Override
    public boolean sendFeishuMarketReviewCard(String reviewName, String reviewTime, String summary, double avgChangePct, String coreSummary) {
        Long userId = StpUtil.getLoginIdAsLong();
        return sendFeishuMarketReviewCard(reviewName, reviewTime, summary, avgChangePct, coreSummary, userId);
    }

    @Override
    public boolean sendFeishuMarketReviewCard(String reviewName, String reviewTime, String summary, double avgChangePct, String coreSummary, Long userId) {
        NotificationConfig config = getActiveFeishuConfig(userId);
        if (config == null) {
            log.warn("未找到活跃的飞书通知配置");
            return false;
        }

        JSONObject card = new JSONObject();
        card.put("config", new JSONObject().put("wide_screen_mode", true));

        JSONObject header = new JSONObject();
        header.put("template", "blue");
        JSONObject title = new JSONObject();
        title.put("tag", "plain_text");
        title.put("content", reviewName);
        header.put("title", title);
        card.put("header", header);

        cn.hutool.json.JSONArray elements = new cn.hutool.json.JSONArray();

        JSONObject timeDiv = new JSONObject();
        timeDiv.put("tag", "div");
        JSONObject timeText = new JSONObject();
        timeText.put("tag", "lark_md");
        timeText.put("content", "**复盘时间：** " + reviewTime);
        timeDiv.put("text", timeText);
        elements.add(timeDiv);

        elements.add(new JSONObject().put("tag", "hr"));

        JSONObject summaryDiv = new JSONObject();
        summaryDiv.put("tag", "div");
        JSONObject summaryText = new JSONObject();
        summaryText.put("tag", "lark_md");
        String summaryIcon = avgChangePct >= 0 ? "" : "";
        summaryText.put("content", "**市场总结**\n\n" + summary + "（" + String.format("%.2f", avgChangePct) + "%）");
        summaryDiv.put("text", summaryText);
        elements.add(summaryDiv);

        elements.add(new JSONObject().put("tag", "hr"));

        if (coreSummary != null && !coreSummary.isBlank()) {
            JSONObject insightDiv = new JSONObject();
            insightDiv.put("tag", "div");
            JSONObject insightText = new JSONObject();
            insightText.put("tag", "lark_md");
            insightText.put("content", "**核心总结**\n\n" + coreSummary);
            insightDiv.put("text", insightText);
            elements.add(insightDiv);
            elements.add(new JSONObject().put("tag", "hr"));
        }

        JSONObject note = new JSONObject();
        note.put("tag", "note");
        cn.hutool.json.JSONArray noteElements = new cn.hutool.json.JSONArray();
        JSONObject noteText = new JSONObject();
        noteText.put("tag", "plain_text");
        noteText.put("content", "完整复盘报告请登录系统查看");
        noteElements.add(noteText);
        note.put("elements", noteElements);
        elements.add(note);

        card.put("elements", elements);

        JSONObject message = new JSONObject();
        message.put("msg_type", "interactive");
        message.put("card", card);

        return doPost(config.getWebhookUrl(), message);
    }

    private boolean doPost(String webhookUrl, JSONObject message) {
        try (HttpResponse response = HttpRequest.post(webhookUrl)
                .charset(StandardCharsets.UTF_8)
                .body(JSONUtil.toJsonStr(message))
                .execute()) {
            String responseBody = response.body();
            JSONObject result = JSONUtil.parseObj(responseBody);
            boolean success = BusinessConstants.FEISHU_SUCCESS_STATUS.equals(result.getStr(ApiConstants.KEY_STATUS));
            if (!success) {
                log.warn("飞书消息发送失败: {}", responseBody);
            }
            return success;
        } catch (Exception e) {
            log.error("飞书消息发送异常", e);
            return false;
        }
    }
}
