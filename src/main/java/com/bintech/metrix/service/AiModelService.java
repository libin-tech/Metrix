package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.AiModelConfigRequest;
import com.bintech.metrix.dto.request.AiModelTestRequest;
import com.bintech.metrix.dto.response.AiModelTestResponse;
import com.bintech.metrix.repository.entity.AiModelConfig;

import java.util.List;

/**
 * AI模型配置服务接口
 *
 * <p>提供AI模型（如OpenAI、DeepSeek等）的配置管理与调用功能，
 * 支持多模型切换、连接测试、分析生成等。
 */
public interface AiModelService {

    /**
     * 创建模型配置
     *
     * @param request 配置请求（模型类型、API Key、端点等）
     * @return 创建后的配置
     */
    AiModelConfig createConfig(AiModelConfigRequest request);

    /**
     * 更新模型配置
     *
     * @param id      配置ID
     * @param request 配置请求
     * @return 更新后的配置
     */
    AiModelConfig updateConfig(Long id, AiModelConfigRequest request);

    /** 根据ID获取配置 */
    AiModelConfig getConfigById(Long id);

    /** 获取所有配置 */
    List<AiModelConfig> getAllConfigs();

    /** 获取已激活的配置列表 */
    List<AiModelConfig> getActiveConfigs();

    /**
     * 获取当前激活的模型类型
     *
     * <p>从数据库中查询当前激活的AI模型配置，如果存在多个激活配置，返回第一个。
     * 如果没有找到激活配置，返回默认值"OPENAI"。
     *
     * @return 当前激活的模型类型
     */
    String getActiveModelType();

    /**
     * 获取当前激活的模型配置
     *
     * <p>从数据库中查询当前激活的AI模型配置，如果存在多个激活配置，返回第一个。
     * 如果没有找到激活配置，返回null。
     *
     * @return 当前激活的模型配置，若无则返回null
     */
    AiModelConfig getActiveConfig();

    /** 删除配置 */
    void deleteConfig(Long id);

    /**
     * 根据模型类型获取激活的配置
     *
     * @param modelType 模型类型（如 OPENAI, DEEPSEEK）
     * @return 激活的配置，不存在时返回null
     */
    AiModelConfig getActiveConfigByType(String modelType);

    /**
     * 生成分析内容
     *
     * @param prompt    提示词
     * @param modelType 模型类型
     * @return 模型返回的分析文本
     */
    String generateAnalysis(String prompt, String modelType);

    /**
     * 测试模型连接
     *
     * @param request 测试请求（模型配置信息）
     * @return 测试结果（连通性、延迟等）
     */
    AiModelTestResponse testConnection(AiModelTestRequest request);
}
