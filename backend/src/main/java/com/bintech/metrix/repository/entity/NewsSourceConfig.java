package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 新闻源配置实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("news_source_config")
public class NewsSourceConfig extends BaseEntity {

    /**
     * 源名称（BOCHA）
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

    /** 用户ID（数据隔离） */
    @TableField(value = "user_id")
    private Long userId;
}
