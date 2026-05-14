package com.bin.stockanalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bin.stockanalysis.dto.request.AiModelConfigRequest;
import com.bin.stockanalysis.dto.request.AiModelTestRequest;
import com.bin.stockanalysis.dto.response.AiModelTestResponse;
import com.bin.stockanalysis.repository.entity.AiModelConfig;
import com.bin.stockanalysis.repository.mapper.AiModelConfigMapper;
import com.bin.stockanalysis.service.AiModelService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    private final AiModelConfigMapper configMapper;

    @Override
    @Transactional
    public AiModelConfig createConfig(AiModelConfigRequest request) {
        if (Boolean.TRUE.equals(request.getIsActive())) {
            deactivateSameType(request.getModelType(), null);
        }
        AiModelConfig config = new AiModelConfig();
        config.setModelType(request.getModelType());
        config.setModelName(request.getModelName());
        config.setApiBaseUrl(request.getApiBaseUrl());
        config.setApiKey(request.getApiKey());
        config.setTemperature(request.getTemperature());
        config.setTimeout(request.getTimeout());
        config.setIsActive(request.getIsActive());
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        configMapper.insert(config);
        return config;
    }

    @Override
    @Transactional
    public AiModelConfig updateConfig(Long id, AiModelConfigRequest request) {
        AiModelConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("AI Model config not found");
        }
        if (Boolean.TRUE.equals(request.getIsActive())) {
            deactivateSameType(request.getModelType(), id);
        }
        config.setModelType(request.getModelType());
        config.setModelName(request.getModelName());
        config.setApiBaseUrl(request.getApiBaseUrl());
        config.setApiKey(request.getApiKey());
        config.setTemperature(request.getTemperature());
        config.setTimeout(request.getTimeout());
        config.setIsActive(request.getIsActive());
        config.setUpdateTime(LocalDateTime.now());
        configMapper.updateById(config);
        return config;
    }

    private void deactivateSameType(String modelType, Long excludeId) {
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getModelType, modelType)
                .eq(AiModelConfig::getIsActive, true);
        if (excludeId != null) {
            wrapper.ne(AiModelConfig::getId, excludeId);
        }
        List<AiModelConfig> activeConfigs = configMapper.selectList(wrapper);
        for (AiModelConfig c : activeConfigs) {
            c.setIsActive(false);
            c.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(c);
        }
    }

    @Override
    public AiModelConfig getConfigById(Long id) {
        AiModelConfig config = configMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("AI Model config not found");
        }
        return config;
    }

    @Override
    public List<AiModelConfig> getAllConfigs() {
        return configMapper.selectList(null);
    }

    @Override
    public List<AiModelConfig> getActiveConfigs() {
        LambdaQueryWrapper<AiModelConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiModelConfig::getIsActive, true);
        return configMapper.selectList(queryWrapper);
    }

    @Override
    public AiModelConfig getActiveConfig() {
        LambdaQueryWrapper<AiModelConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiModelConfig::getIsActive, true)
                .orderByDesc(AiModelConfig::getUpdateTime)
                .last("LIMIT 1");
        return configMapper.selectOne(queryWrapper);
    }

    @Override
    public String getActiveModelType() {
        try {
            AiModelConfig config = getActiveConfig();
            if (config != null && config.getModelType() != null && !config.getModelType().isEmpty()) {
                log.info("从数据库获取到激活的模型类型: {}", config.getModelType());
                return config.getModelType();
            }
            log.warn("未找到激活的模型配置，使用默认值: OPENAI");
            return "OPENAI";
        } catch (Exception e) {
            log.error("获取激活模型类型时发生异常: {}", e.getMessage());
            log.warn("异常情况下使用默认模型类型: OPENAI");
            return "OPENAI";
        }
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) {
        configMapper.deleteById(id);
    }

    @Override
    public AiModelConfig getActiveConfigByType(String modelType) {
        LambdaQueryWrapper<AiModelConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AiModelConfig::getModelType, modelType)
                .eq(AiModelConfig::getIsActive, true);
        AiModelConfig config = configMapper.selectOne(queryWrapper);
        if (config == null) {
            throw new RuntimeException("Active AI Model config not found for type: " + modelType);
        }
        return config;
    }

    @Override
    public String generateAnalysis(String prompt, String modelType) {
        AiModelConfig config = getActiveConfigByType(modelType);
        String modelName = config.getModelName();
        int promptLength = prompt != null ? prompt.length() : 0;
        String promptPreview = prompt != null && prompt.length() > 50 ? prompt.substring(0, 50) + "..." : prompt;
        log.info("===== AI分析开始 =====");
        log.info("模型类型: {}, 模型名称: {}, 提示词长度: {}字符", modelType, modelName, promptLength);
        log.info("提示词预览: {}", promptPreview);

        long startTime = System.currentTimeMillis();
        ChatLanguageModel model = buildModel(config, modelType);
        String content = model.generate(prompt);
        long duration = System.currentTimeMillis() - startTime;

        int contentLength = content != null ? content.length() : 0;
        log.info("===== AI分析完成 =====");
        log.info("耗时: {}秒, 生成内容长度: {}字符", duration / 1000.0, contentLength);
        return content;
    }

    /**
     * 构建ChatLanguageModel实例
     * 
     * @param config 模型配置
     * @param modelType 模型类型
     * @return ChatLanguageModel实例
     */
    private ChatLanguageModel buildModel(AiModelConfig config, String modelType) {
        if ("OLLAMA".equalsIgnoreCase(modelType)) {
            return OllamaChatModel.builder()
                    .baseUrl(config.getApiBaseUrl())
                    .modelName(config.getModelName())
                    .temperature(config.getTemperature())
                    .build();
        } else {
            OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                    .baseUrl(config.getApiBaseUrl())
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .temperature(config.getTemperature());
            if (config.getTimeout() != null) {
                builder.timeout(Duration.ofSeconds(config.getTimeout()));
            }
            return builder.build();
        }
    }

    @Override
    public AiModelTestResponse testConnection(AiModelTestRequest request) {
        ChatLanguageModel model;

        if ("OLLAMA".equalsIgnoreCase(request.getModelType())) {
            model = OllamaChatModel.builder()
                    .baseUrl(request.getApiBaseUrl())
                    .modelName(request.getModelName())
                    .temperature(request.getTemperature())
                    .build();
        } else {
            OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                    .baseUrl(request.getApiBaseUrl())
                    .apiKey(request.getApiKey())
                    .modelName(request.getModelName())
                    .temperature(request.getTemperature());
            if (request.getTimeout() != null) {
                builder.timeout(Duration.ofSeconds(request.getTimeout()));
            }
            model = builder.build();
        }

        long start = System.currentTimeMillis();
        model.generate("Say just 'ok'");
        long elapsed = System.currentTimeMillis() - start;

        return new AiModelTestResponse(request.getModelName(), elapsed);
    }
}
