package com.bin.stockanalysis.service;

import com.bin.stockanalysis.dto.request.AiModelConfigRequest;
import com.bin.stockanalysis.dto.request.AiModelTestRequest;
import com.bin.stockanalysis.dto.response.AiAnalysisResult;
import com.bin.stockanalysis.dto.response.AiModelTestResponse;
import com.bin.stockanalysis.repository.entity.AiModelConfig;

import java.util.List;

public interface AiModelService {
    AiModelConfig createConfig(AiModelConfigRequest request);
    AiModelConfig updateConfig(Long id, AiModelConfigRequest request);
    AiModelConfig getConfigById(Long id);
    List<AiModelConfig> getAllConfigs();
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
    
    void deleteConfig(Long id);
    AiModelConfig getActiveConfigByType(String modelType);
    String generateAnalysis(String prompt, String modelType);
    
    /**
     * 生成分析结果并返回包含置信度的响应
     * 
     * <p>调用AI模型生成分析内容，并尝试从模型响应中提取或计算置信度。
     * 如果模型不支持返回置信度，则根据数据完整性计算一个综合置信度。
     * 
     * @param prompt 分析提示词
     * @param modelType 模型类型
     * @return AI分析结果，包含分析内容和置信度
     */
    AiAnalysisResult generateAnalysisWithConfidence(String prompt, String modelType);
    
    AiModelTestResponse testConnection(AiModelTestRequest request);
}
