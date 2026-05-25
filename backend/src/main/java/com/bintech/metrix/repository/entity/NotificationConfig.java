package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 通知配置实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("notification_config")
public class NotificationConfig extends BaseEntity {

    /**
     * 渠道类型（FEISHU）
     */
    @TableField(value = "channel_type")
    private String channelType;

    /**
     * WebHook URL
     */
    @TableField(value = "webhook_url")
    private String webhookUrl;

    /**
     * 密钥
     */
    @TableField(value = "secret")
    private String secret;

    /**
     * 是否激活
     */
    @TableField(value = "is_active")
    private Boolean isActive;
}
