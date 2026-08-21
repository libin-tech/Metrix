package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.AiModelConfigRequest;
import com.bintech.metrix.dto.request.AiModelTestRequest;
import com.bintech.metrix.dto.response.AiModelTestResponse;
import com.bintech.metrix.dto.response.AnalysisResult;
import com.bintech.metrix.repository.entity.AiModelConfig;

import java.util.List;
import java.util.function.Consumer;

/**
 * AI模型配置服务接口
 *
 * <p>提供AI模型（如OpenAI、DeepSeek等）的配置管理与调用功能，
 * 支持多模型切换、连接测试、分析生成等。
 */
public interface AiModelService {

    AiModelConfig createConfig(AiModelConfigRequest request);

    AiModelConfig updateConfig(Long id, AiModelConfigRequest request);

    AiModelConfig getConfigById(Long id);

    List<AiModelConfig> getAllConfigs();

    List<AiModelConfig> getAllConfigs(Long userId);

    List<AiModelConfig> getActiveConfigs();

    List<AiModelConfig> getActiveConfigs(Long userId);

    String getActiveModelType();

    String getActiveModelType(Long userId);

    AiModelConfig getActiveConfig();

    AiModelConfig getActiveConfig(Long userId);

    void deleteConfig(Long id);

    AiModelConfig getActiveConfigByType(String modelType);

    AiModelConfig getActiveConfigByType(String modelType, Long userId);

    String generateAnalysis(String prompt, String modelType);

    String generateAnalysis(String prompt, String modelType, Long userId);

    void generateAnalysisStreaming(String prompt, String modelType,
                                    Consumer<String> onNext,
                                    Consumer<AnalysisResult> onComplete,
                                    Consumer<Throwable> onError);

    void generateAnalysisStreaming(String prompt, String modelType, Long userId,
                                    Consumer<String> onNext,
                                    Consumer<AnalysisResult> onComplete,
                                    Consumer<Throwable> onError);

    boolean hasActiveConfig(Long userId);

    AiModelTestResponse testConnection(AiModelTestRequest request);
}
