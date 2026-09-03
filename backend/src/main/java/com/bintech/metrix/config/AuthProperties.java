package com.bintech.metrix.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 认证与验证码配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** 是否启用所有图形验证码校验。 */
    private boolean captchaEnabled = true;

    /** 验证邮件发件人。 */
    private String mailFrom = "";
}
