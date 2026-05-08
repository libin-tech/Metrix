package com.bin.stockanalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bin.stockanalysis.dto.request.AiModelConfigRequest;
import com.bin.stockanalysis.dto.request.AiModelTestRequest;
import com.bin.stockanalysis.dto.response.AiAnalysisResult;
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

import java.math.BigDecimal;
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
        ChatLanguageModel model = buildModel(config, modelType);
        return model.generate(prompt);
    }

    @Override
    public AiAnalysisResult generateAnalysisWithConfidence(String prompt, String modelType) {
        AiModelConfig config = getActiveConfigByType(modelType);
        ChatLanguageModel model = buildModel(config, modelType);

        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 调用模型生成分析
        String content = model.generate(prompt);
        
        // 计算响应时间
        long responseTime = System.currentTimeMillis() - startTime;
        
        // 计算置信度
        BigDecimal confidenceScore = calculateConfidenceScore(prompt, content, config);
        
        log.info("AI分析完成，模型: {}, 置信度: {}, 响应时间: {}ms", 
                config.getModelName(), confidenceScore, responseTime);

        return AiAnalysisResult.builder()
                .content(content)
                .confidenceScore(confidenceScore)
                .modelName(config.getModelName())
                .responseTime(responseTime)
                .build();
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
            return OpenAiChatModel.builder()
                    .baseUrl(config.getApiBaseUrl())
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .temperature(config.getTemperature())
                    .build();
        }
    }

    /**
     * 计算置信度分数
     * 
     * <p>基于多个因素综合计算置信度：
     * <ul>
     *   <li>提示词质量（长度、信息丰富度）：占40%</li>
     *   <li>响应质量（长度、完整性）：占30%</li>
     *   <li>模型配置（温度参数）：占30%</li>
     * </ul>
     * 
     * @param prompt 提示词
     * @param response 模型响应
     * @param config 模型配置
     * @return 置信度分数（0-1）
     */
    private BigDecimal calculateConfidenceScore(String prompt, String response, AiModelConfig config) {
        // 基础置信度（基于温度参数，温度越低越确定）
        double baseScore = 0.70;
        if (config.getTemperature() != null) {
            // 温度越低，置信度越高（温度范围通常0-2）
            double temperature = config.getTemperature();
            baseScore = Math.max(0.5, 0.9 - temperature * 0.2);
        }

        // 提示词质量评分（占40%权重）
        double promptScore = calculatePromptScore(prompt);
        
        // 响应质量评分（占30%权重）
        double responseScore = calculateResponseScore(response);
        
        // 综合计算置信度
        double confidence = baseScore * 0.3 + promptScore * 0.4 + responseScore * 0.3;
        
        // 限制置信度在0.5到0.95之间
        confidence = Math.max(0.5, Math.min(0.95, confidence));
        
        return BigDecimal.valueOf(confidence).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 计算提示词质量分数
     * 
     * @param prompt 提示词
     * @return 分数（0-1）
     */
    private double calculatePromptScore(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return 0.3;
        }
        
        // 根据提示词长度评分
        int length = prompt.length();
        
        if (length < 100) {
            return 0.4; // 简短提示词
        } else if (length < 500) {
            return 0.7; // 中等长度提示词
        } else if (length < 1000) {
            return 0.9; // 详细提示词
        } else {
            return 1.0; // 非常详细的提示词
        }
    }

    /**
     * 计算响应质量分数
     * 
     * @param response 模型响应
     * @return 分数（0-1）
     */
    private double calculateResponseScore(String response) {
        if (response == null || response.isEmpty()) {
            return 0.2;
        }
        
        int length = response.length();
        
        // 检查是否包含结构化内容（如列表、分点等）
        boolean hasStructure = response.contains("1.") || response.contains("- ") || 
                               response.contains("##") || response.contains("###");
        
        if (length < 200) {
            return hasStructure ? 0.5 : 0.3;
        } else if (length < 500) {
            return hasStructure ? 0.8 : 0.6;
        } else {
            return hasStructure ? 1.0 : 0.8;
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
            model = OpenAiChatModel.builder()
                    .baseUrl(request.getApiBaseUrl())
                    .apiKey(request.getApiKey())
                    .modelName(request.getModelName())
                    .temperature(request.getTemperature())
                    .build();
        }

        long start = System.currentTimeMillis();
        model.generate("Say just 'ok'");
        long elapsed = System.currentTimeMillis() - start;

        return new AiModelTestResponse(request.getModelName(), elapsed);
    }
}
