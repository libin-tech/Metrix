package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * AI模型配置实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model_config")
public class AiModelConfig extends BaseEntity {

    /**
     * 模型类型（OPENAI/OLLAMA/GEMINI）
     */
    @TableField(value = "model_type")
    private String modelType;

    /**
     * 模型名称
     */
    @TableField(value = "model_name")
    private String modelName;

    /**
     * API基础URL
     */
    @TableField(value = "api_base_url")
    private String apiBaseUrl;

    /**
     * API密钥
     */
    @TableField(value = "api_key")
    private String apiKey;

    /**
     * 温度参数
     */
    @TableField(value = "temperature")
    private Double temperature;

    /**
     * 是否激活
     */
    @TableField(value = "is_active")
    private Boolean isActive;

    /**
     * 超时时间（秒）
     */
    @TableField(value = "timeout")
    private Integer timeout;
}
