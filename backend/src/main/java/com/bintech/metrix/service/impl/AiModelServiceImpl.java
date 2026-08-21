package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.constants.SystemConstants;
import com.bintech.metrix.dto.request.AiModelConfigRequest;
import com.bintech.metrix.dto.request.AiModelTestRequest;
import com.bintech.metrix.dto.response.AiModelTestResponse;
import com.bintech.metrix.dto.response.AnalysisResult;
import com.bintech.metrix.repository.dao.AiModelConfigDao;
import com.bintech.metrix.repository.entity.AiModelConfig;
import com.bintech.metrix.service.AiModelService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelServiceImpl implements AiModelService {

    private final AiModelConfigDao configDao;

    @Override
    @Transactional
    public AiModelConfig createConfig(AiModelConfigRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (Boolean.TRUE.equals(request.getIsActive())) {
            deactivateSameType(request.getModelType(), null, userId);
        }
        AiModelConfig config = new AiModelConfig();
        config.setModelType(request.getModelType());
        config.setModelName(request.getModelName());
        config.setApiBaseUrl(request.getApiBaseUrl());
        config.setApiKey(request.getApiKey());
        config.setTemperature(request.getTemperature());
        config.setTimeout(request.getTimeout());
        config.setIsActive(request.getIsActive());
        config.setUserId(userId);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        configDao.insert(config);
        return config;
    }

    @Override
    @Transactional
    public AiModelConfig updateConfig(Long id, AiModelConfigRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        AiModelConfig config = configDao.selectByIdAndUserId(id, userId);
        if (config == null) {
            throw new RuntimeException("AI Model config not found");
        }
        if (Boolean.TRUE.equals(request.getIsActive())) {
            deactivateSameType(request.getModelType(), id, userId);
        }
        config.setModelType(request.getModelType());
        config.setModelName(request.getModelName());
        config.setApiBaseUrl(request.getApiBaseUrl());
        config.setApiKey(request.getApiKey());
        config.setTemperature(request.getTemperature());
        config.setTimeout(request.getTimeout());
        config.setIsActive(request.getIsActive());
        config.setUpdateTime(LocalDateTime.now());
        configDao.updateById(config);
        return config;
    }

    private void deactivateSameType(String modelType, Long excludeId, Long userId) {
        List<AiModelConfig> activeConfigs = configDao.selectActiveByUserIdAndModelType(userId, modelType);
        for (AiModelConfig c : activeConfigs) {
            if (excludeId != null && c.getId().equals(excludeId)) {
                continue;
            }
            c.setIsActive(false);
            c.setUpdateTime(LocalDateTime.now());
            configDao.updateById(c);
        }
    }

    @Override
    public AiModelConfig getConfigById(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        AiModelConfig config = configDao.selectByIdAndUserId(id, userId);
        if (config == null) {
            throw new RuntimeException("AI Model config not found");
        }
        return config;
    }

    @Override
    public List<AiModelConfig> getAllConfigs() {
        Long userId = StpUtil.getLoginIdAsLong();
        return getAllConfigs(userId);
    }

    @Override
    public List<AiModelConfig> getAllConfigs(Long userId) {
        return configDao.selectByUserId(userId);
    }

    @Override
    public List<AiModelConfig> getActiveConfigs() {
        Long userId = StpUtil.getLoginIdAsLong();
        return getActiveConfigs(userId);
    }

    @Override
    public List<AiModelConfig> getActiveConfigs(Long userId) {
        return configDao.selectActiveByUserId(userId);
    }

    @Override
    public AiModelConfig getActiveConfig() {
        Long userId = StpUtil.getLoginIdAsLong();
        return getActiveConfig(userId);
    }

    @Override
    public AiModelConfig getActiveConfig(Long userId) {
        if (userId == null) {
            return null;
        }
        List<AiModelConfig> configs = configDao.selectActiveByUserId(userId);
        if (configs.isEmpty()) {
            return null;
        }
        return configs.stream()
                .max(Comparator.comparing(AiModelConfig::getUpdateTime))
                .orElse(null);
    }

    @Override
    public String getActiveModelType() {
        Long userId = StpUtil.getLoginIdAsLong();
        return getActiveModelType(userId);
    }

    @Override
    public String getActiveModelType(Long userId) {
        try {
            AiModelConfig config = getActiveConfig(userId);
            if (config != null && config.getModelType() != null && !config.getModelType().isEmpty()) {
                log.info("从数据库获取到激活的模型类型: {} (userId={})", config.getModelType(), userId);
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
        Long userId = StpUtil.getLoginIdAsLong();
        long count = configDao.countByIdAndUserId(id, userId);
        if (count == 0) {
            throw new RuntimeException("AI Model config not found");
        }
        configDao.deleteById(id);
    }

    @Override
    public AiModelConfig getActiveConfigByType(String modelType) {
        Long userId = StpUtil.getLoginIdAsLong();
        return getActiveConfigByType(modelType, userId);
    }

    @Override
    public AiModelConfig getActiveConfigByType(String modelType, Long userId) {
        if (userId == null) {
            return null;
        }
        AiModelConfig config = configDao.selectOneActiveByUserIdAndModelType(userId, modelType);
        if (config == null) {
            throw new RuntimeException("Active AI Model config not found for type: " + modelType);
        }
        return config;
    }

    @Override
    public String generateAnalysis(String prompt, String modelType) {
        Long userId = StpUtil.getLoginIdAsLong();
        return generateAnalysis(prompt, modelType, userId);
    }

    @Override
    public String generateAnalysis(String prompt, String modelType, Long userId) {
        AiModelConfig config = getActiveConfigByType(modelType, userId);
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

    @Override
    public void generateAnalysisStreaming(String prompt, String modelType,
                                          java.util.function.Consumer<String> onNext,
                                          java.util.function.Consumer<AnalysisResult> onComplete,
                                          java.util.function.Consumer<Throwable> onError) {
        Long userId = StpUtil.getLoginIdAsLong();
        generateAnalysisStreaming(prompt, modelType, userId, onNext, onComplete, onError);
    }

    @Override
    public void generateAnalysisStreaming(String prompt, String modelType, Long userId,
                                          java.util.function.Consumer<String> onNext,
                                          java.util.function.Consumer<AnalysisResult> onComplete,
                                          java.util.function.Consumer<Throwable> onError) {
        AiModelConfig config = getActiveConfigByType(modelType, userId);
        String modelName = config.getModelName();
        log.info("===== AI流式分析开始 =====");
        log.info("模型类型: {}, 模型名称: {}", modelType, modelName);

        long startTime = System.currentTimeMillis();
        StringBuilder fullContent = new StringBuilder();
        StreamingChatModel model = buildStreamingModel(config, modelType);

        model.chat(prompt, new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                fullContent.append(partialResponse);
                onNext.accept(partialResponse);
            }

            @Override
            public void onCompleteResponse(dev.langchain4j.model.chat.response.ChatResponse completeResponse) {
                long duration = System.currentTimeMillis() - startTime;
                String content = fullContent.toString();
                int totalTokens = 0;
                try {
                    if (completeResponse != null
                            && completeResponse.metadata() != null
                            && completeResponse.metadata().tokenUsage() != null) {
                        Integer tc = completeResponse.metadata().tokenUsage().totalTokenCount();
                        totalTokens = tc != null ? tc : content.length() / 2;
                    } else {
                        totalTokens = content.length() / 2;
                    }
                } catch (Exception e) {
                    totalTokens = content.length() / 2;
                }
                log.info("===== AI流式分析完成 =====");
                log.info("耗时: {}秒, 生成内容长度: {}字符, Token数: {}",
                        duration / (double) SystemConstants.MILLIS_PER_SECOND, content.length(), totalTokens);
                onComplete.accept(new AnalysisResult(content, totalTokens));
            }

            @Override
            public void onError(Throwable error) {
                log.error("AI流式分析出错: {}", error.getMessage(), error);
                onError.accept(error);
            }
        });
    }

    private StreamingChatModel buildStreamingModel(AiModelConfig config, String modelType) {
        if (BusinessConstants.MODEL_TYPE_OLLAMA.equalsIgnoreCase(modelType)) {
            return OllamaStreamingChatModel.builder()
                    .baseUrl(config.getApiBaseUrl())
                    .modelName(config.getModelName())
                    .temperature(config.getTemperature())
                    .timeout(resolveTimeout(config))
                    .build();
        }
        if (BusinessConstants.MODEL_TYPE_GEMINI.equalsIgnoreCase(modelType)) {
            GoogleAiGeminiStreamingChatModel.GoogleAiGeminiStreamingChatModelBuilder geminiBuilder =
                    GoogleAiGeminiStreamingChatModel.builder()
                            .apiKey(config.getApiKey())
                            .modelName(config.getModelName())
                            .temperature(config.getTemperature());
            if (config.getTimeout() != null) {
                geminiBuilder.timeout(java.time.Duration.ofSeconds(config.getTimeout()));
            }
            return geminiBuilder.build();
        }
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .baseUrl(config.getApiBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .temperature(config.getTemperature());
        if (config.getTimeout() != null) {
            builder.timeout(java.time.Duration.ofSeconds(config.getTimeout()));
        }
        return builder.build();
    }

    private ChatModel buildModel(AiModelConfig config, String modelType) {
        if (BusinessConstants.MODEL_TYPE_OLLAMA.equalsIgnoreCase(modelType)) {
            return OllamaChatModel.builder()
                    .baseUrl(config.getApiBaseUrl())
                    .modelName(config.getModelName())
                    .temperature(config.getTemperature())
                    .timeout(resolveTimeout(config))
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

    private Duration resolveTimeout(AiModelConfig config) {
        int timeoutSeconds = config.getTimeout() == null ? SystemConstants.AI_MODEL_TIMEOUT_SECONDS : config.getTimeout();
        return Duration.ofSeconds(timeoutSeconds);
    }

    @Override
    public boolean hasActiveConfig(Long userId) {
        return getActiveConfig(userId) != null;
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

            OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                    .baseUrl(request.getApiBaseUrl())
                    .apiKey(request.getApiKey())
                    .modelName(request.getModelName())
                    .temperature(request.getTemperature());
            if (request.getTimeout() != null) {
                builder.timeout(Duration.ofSeconds(request.getTimeout()));
            }
            model = builder.build();
            log.info("OpenAI 兼容模型实例创建完成");
        }

        long start = System.currentTimeMillis();
        try {
            log.info("开始执行测试连接请求...");

            String result = model.chat(BusinessConstants.TEST_CONNECTION_PROMPT);

            long elapsed = System.currentTimeMillis() - start;
            log.info("===== AI模型连接测试完成 =====");

            return new AiModelTestResponse(request.getModelName(), elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("测试连接失败");

            String errorMsg = "AI模型连接测试失败: ";
            if (e.getMessage() != null && (e.getMessage().contains("ConnectException") || e.getMessage().contains("ClosedChannelException"))) {
                if (BusinessConstants.MODEL_TYPE_GEMINI.equalsIgnoreCase(request.getModelType())) {
                    errorMsg += "无法连接到 Gemini API 服务器。可能原因：\n" +
                            "1. 网络连接问题\n" +
                            "2. 需要配置网络代理\n" +
                            "3. API Key 可能无效或已过期\n" +
                            "4. 防火墙阻止了连接";
                } else if (BusinessConstants.MODEL_TYPE_OLLAMA.equalsIgnoreCase(request.getModelType())) {
                    errorMsg += "无法连接到 Ollama 服务器。请检查：\n" +
                            "1. Ollama 服务是否正在运行\n" +
                            "2. API Base URL 是否正确: " + request.getApiBaseUrl() + "\n" +
                            "3. 端口是否正确开放";
                } else {
                    errorMsg += "无法连接到 API 服务器。请检查：\n" +
                            "1. API Base URL 是否正确: " + request.getApiBaseUrl() + "\n" +
                            "2. 服务是否正在运行\n" +
                            "3. API Key 是否有效";
                }
            } else {
                errorMsg += e.getMessage();
            }

            throw new RuntimeException(errorMsg, e);
        }
    }

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
