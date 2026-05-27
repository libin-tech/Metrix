package com.bintech.metrix.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 微信公众号配置类
 *
 * 从application.yml或application.properties中读取wechat前缀的配置项，
 * 用于微信公众号相关的认证和消息处理
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "wechat")
public class WechatConfig {

    /**
     * 微信公众号AppID
     * 公众号的唯一标识，由微信平台分配
     */
    private String appId;

    /**
     * 微信公众号AppSecret
     * 公众号的密钥，用于接口调用鉴权
     */
    private String appSecret;

    /**
     * 微信公众号Token
     * 用于验证服务器地址的有效性，在公众号后台配置
     */
    private String token;

    /**
     * 微信公众号EncodingAESKey
     * 消息加解密密钥，用于安全模式下的消息加密解密
     */
    private String encodingAesKey;
}
