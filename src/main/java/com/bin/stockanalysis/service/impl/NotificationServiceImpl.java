package com.bin.stockanalysis.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bin.stockanalysis.dto.request.NotificationConfigRequest;
import com.bin.stockanalysis.repository.entity.NotificationConfig;
import com.bin.stockanalysis.repository.mapper.NotificationConfigMapper;
import com.bin.stockanalysis.service.NotificationService;
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

    private final NotificationConfigMapper configMapper;

    @Override
    @Transactional
    public NotificationConfig createConfig(NotificationConfigRequest request) {
        NotificationConfig config = new NotificationConfig();
        config.setChannelType(request.getChannelType());
        config.setWebhookUrl(request.getWebhookUrl());
        config.setSecret(request.getSecret());
        config.setIsActive(request.getIsActive());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        configMapper.insert(config);
        return config;
    }

    @Override
    @Transactional
    public NotificationConfig updateConfig(Long id, NotificationConfigRequest request) {
        NotificationConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("Notification config not found");
        }
        config.setChannelType(request.getChannelType());
        config.setWebhookUrl(request.getWebhookUrl());
        config.setSecret(request.getSecret());
        config.setIsActive(request.getIsActive());
        config.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(config);
        return config;
    }

    @Override
    public NotificationConfig getConfigById(Long id) {
        NotificationConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("Notification config not found");
        }
        return config;
    }

    @Override
    public List<NotificationConfig> getAllConfigs() {
        return configMapper.selectList(null);
    }

    @Override
    public List<NotificationConfig> getActiveConfigs() {
        LambdaQueryWrapper<NotificationConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NotificationConfig::getIsActive, true);
        return configMapper.selectList(queryWrapper);
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        configMapper.deleteById(id);
    }

    @Override
    public boolean sendFeishuNotification(String title, String content) {
        return sendFeishuNotification(title, content, null);
    }

    @Override
    public boolean sendFeishuNotification(String title, String content, Map<String, Object> attachments) {
        LambdaQueryWrapper<NotificationConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NotificationConfig::getChannelType, "FEISHU")
                .eq(NotificationConfig::getIsActive, true);
        NotificationConfig config = configMapper.selectOne(queryWrapper);
        if (config == null) {
            throw new RuntimeException("Feishu notification config not found");
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

        try (HttpResponse response = HttpRequest.post(config.getWebhookUrl())
                .charset(StandardCharsets.UTF_8)
                .body(JSONUtil.toJsonStr(message))
                .execute()) {
            
            String responseBody = response.body();
            JSONObject result = JSONUtil.parseObj(responseBody);
            return "ok".equals(result.getStr("status"));
        } catch (Exception e) {
            log.error("Failed to send Feishu notification", e);
            return false;
        }
    }
}
