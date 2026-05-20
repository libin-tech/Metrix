package com.bintech.metrix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.dto.request.AiModelConfigRequest;
import com.bintech.metrix.dto.request.AiModelTestRequest;
import com.bintech.metrix.dto.response.AiModelTestResponse;
import com.bintech.metrix.repository.entity.AiModelConfig;
import com.bintech.metrix.repository.mapper.AiModelConfigMapper;
import com.bintech.metrix.service.AiModelService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
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
            log.warn("未找到激活的模型配置，使用默认值: {}", BusinessConstants.MODEL_TYPE_OPENAI);
            return BusinessConstants.MODEL_TYPE_OPENAI;
        } catch (Exception e) {
            log.error("获取激活模型类型时发生异常: {}", e.getMessage());
            log.warn("异常情况下使用默认模型类型: {}", BusinessConstants.MODEL_TYPE_OPENAI);
            return BusinessConstants.MODEL_TYPE_OPENAI;
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
        String promptPreview = prompt != null && prompt.length() > SystemConstants.PROMPT_PREVIEW_MAX_LENGTH ? prompt.substring(0, SystemConstants.PROMPT_PREVIEW_MAX_LENGTH) + "..." : prompt;
        log.info("===== AI分析开始 =====");
        log.info("模型类型: {}, 模型名称: {}, 提示词长度: {}字符", modelType, modelName, promptLength);
        log.info("提示词预览: {}", promptPreview);

        long startTime = System.currentTimeMillis();
        ChatModel model = buildModel(config, modelType);
        String content = model.chat(prompt);
        long duration = System.currentTimeMillis() - startTime;

        int contentLength = content != null ? content.length() : 0;
        log.info("===== AI分析完成 =====");
        log.info("耗时: {}秒, 生成内容长度: {}字符", duration / (double) SystemConstants.MILLIS_PER_SECOND, contentLength);
        return content;
    }

    /**
     * 构建ChatLanguageModel实例
     * 
     * @param config 模型配置
     * @param modelType 模型类型
     * @return ChatLanguageModel实例
     */
    private ChatModel buildModel(AiModelConfig config, String modelType) {
        if (BusinessConstants.MODEL_TYPE_OLLAMA.equalsIgnoreCase(modelType)) {
            return OllamaChatModel.builder()
                    .baseUrl(config.getApiBaseUrl())
                    .modelName(config.getModelName())
                    .temperature(config.getTemperature())
                    .build();
        }
        if (BusinessConstants.MODEL_TYPE_GEMINI.equalsIgnoreCase(modelType)) {
            GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder geminiBuilder = GoogleAiGeminiChatModel.builder()
                    .apiKey(config.getApiKey())
                    .modelName(config.getModelName())
                    .temperature(config.getTemperature());
            if (config.getTimeout() != null) {
                geminiBuilder.timeout(Duration.ofSeconds(config.getTimeout()));
            }
            return geminiBuilder.build();
        }
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

    @Override
    public AiModelTestResponse testConnection(AiModelTestRequest request) {
        log.info("===== AI模型连接测试开始 =====");
        log.info("模型类型: {}", request.getModelType());
        log.info("模型名称: {}", request.getModelName());

        ChatModel model;

        if (BusinessConstants.MODEL_TYPE_OLLAMA.equalsIgnoreCase(request.getModelType())) {
            log.info("使用 Ollama 模型配置");
            log.info("API Base URL: {}", request.getApiBaseUrl());
            log.info("温度参数: {}", request.getTemperature());

            model = OllamaChatModel.builder()
                    .baseUrl(request.getApiBaseUrl())
                    .modelName(request.getModelName())
                    .temperature(request.getTemperature())
                    .build();
        } else if (BusinessConstants.MODEL_TYPE_GEMINI.equalsIgnoreCase(request.getModelType())) {
            log.info("使用 Gemini 模型配置");
            log.info("API Key: {}", maskApiKey(request.getApiKey()));
            log.info("模型名称: {}", request.getModelName());
            log.info("温度参数: {}", request.getTemperature());

            if (request.getApiKey() == null || request.getApiKey().trim().isEmpty()) {
                log.error("Gemini API Key 为空");
                throw new RuntimeException("Gemini API Key 不能为空");
            }

            GoogleAiGeminiChatModel.GoogleAiGeminiChatModelBuilder geminiBuilder = GoogleAiGeminiChatModel.builder()
                    .apiKey(request.getApiKey())
                    .modelName(request.getModelName())
                    .temperature(request.getTemperature());

            Integer timeout = request.getTimeout();
            if (timeout == null || timeout <= 0) {
                timeout = 60;
                log.info("未设置超时或超时值无效，使用默认超时: {}秒", timeout);
            } else {
                log.info("超时设置: {}秒", timeout);
            }
            geminiBuilder.timeout(Duration.ofSeconds(timeout));

            model = geminiBuilder.build();
            log.info("Gemini 模型实例创建完成");
        } else {
            log.info("使用 OpenAI 兼容模型配置");
            log.info("API Base URL: {}", request.getApiBaseUrl());
            log.info("API Key: {}", maskApiKey(request.getApiKey()));
            log.info("模型名称: {}", request.getModelName());
            log.info("温度参数: {}", request.getTemperature());

            OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                    .baseUrl(request.getApiBaseUrl())
                    .apiKey(request.getApiKey())
                    .modelName(request.getModelName())
                    .temperature(request.getTemperature());
            if (request.getTimeout() != null) {
                log.info("超时设置: {}秒", request.getTimeout());
                builder.timeout(Duration.ofSeconds(request.getTimeout()));
            }
            model = builder.build();
            log.info("OpenAI 兼容模型实例创建完成");
        }

        long start = System.currentTimeMillis();
        try {
            log.info("开始执行测试连接请求...");
            log.info("测试提示词: {}", BusinessConstants.TEST_CONNECTION_PROMPT);

            String result = model.chat(BusinessConstants.TEST_CONNECTION_PROMPT);

            long elapsed = System.currentTimeMillis() - start;
            log.info("测试连接成功");
            log.info("响应结果: {}", result);
            log.info("总耗时: {}ms ({}秒)", elapsed, elapsed / 1000.0);
            log.info("===== AI模型连接测试完成 =====");

            return new AiModelTestResponse(request.getModelName(), elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("测试连接失败");
            log.error("耗时: {}ms", elapsed);
            log.error("异常类型: {}", e.getClass().getName());
            log.error("异常消息: {}", e.getMessage());

            if (e.getCause() != null) {
                log.error("根本原因: {}", e.getCause().getClass().getName());
                log.error("根本原因消息: {}", e.getCause().getMessage());
            }

            String errorMsg = "AI模型连接测试失败: ";
            if (e.getMessage() != null && (e.getMessage().contains("ConnectException") || e.getMessage().contains("ClosedChannelException"))) {
                if (BusinessConstants.MODEL_TYPE_GEMINI.equalsIgnoreCase(request.getModelType())) {
                    log.error("检测到 Gemini API 连接异常");
                    errorMsg += "无法连接到 Gemini API 服务器。可能原因：\n" +
                            "1. 网络连接问题，请检查是否可以访问 Google API (generativelanguage.googleapis.com)\n" +
                            "2. 需要配置网络代理才能访问 Google 服务\n" +
                            "3. API Key 可能无效或已过期\n" +
                            "4. 防火墙阻止了连接\n" +
                            "5. 当前网络环境不支持访问 Google 服务";
                } else if (BusinessConstants.MODEL_TYPE_OLLAMA.equalsIgnoreCase(request.getModelType())) {
                    log.error("检测到 Ollama 连接异常");
                    errorMsg += "无法连接到 Ollama 服务器。请检查：\n" +
                            "1. Ollama 服务是否正在运行\n" +
                            "2. API Base URL 是否正确: " + request.getApiBaseUrl() + "\n" +
                            "3. 网络连接是否正常\n" +
                            "4. 端口是否正确开放";
                } else {
                    log.error("检测到 OpenAI 兼容 API 连接异常");
                    errorMsg += "无法连接到 API 服务器。请检查：\n" +
                            "1. API Base URL 是否正确: " + request.getApiBaseUrl() + "\n" +
                            "2. 网络连接是否正常\n" +
                            "3. 服务是否正在运行\n" +
                            "4. API Key 是否有效";
                }
            } else {
                errorMsg += e.getMessage();
            }

            log.error("错误详情: {}", errorMsg);
            log.error("===== AI模型连接测试失败 =====");

            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * 隐藏 API Key 用于日志输出
     * @param apiKey 原始 API Key
     * @return 隐藏后的字符串
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "(空)";
        }
        if (apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "..." + apiKey.substring(apiKey.length() - 4);
    }
}
