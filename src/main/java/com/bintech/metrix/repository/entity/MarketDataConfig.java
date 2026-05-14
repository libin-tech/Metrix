package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 市场数据配置实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("market_data_config")
public class MarketDataConfig extends BaseEntity {

    /**
     * 源名称（TICKFLOW）
     */
    @TableField(value = "source_name")
    private String sourceName;

    /**
     * API URL
     */
    @TableField(value = "api_url")
    private String apiUrl;

    /**
     * API密钥
     */
    @TableField(value = "api_key")
    private String apiKey;

    /**
     * 数据类型
     */
    @TableField(value = "data_type")
    private String dataType;

    /**
     * 请求间隔（秒）
     */
    @TableField(value = "request_interval")
    private Integer requestInterval;

    /**
     * 是否激活
     */
    @TableField(value = "is_active")
    private Boolean isActive;

    /**
     * 请求超时时间（秒）
     */
    @TableField(value = "timeout")
    private Integer timeout;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;
}
