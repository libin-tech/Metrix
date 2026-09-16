package com.bintech.metrix.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.interceptor.SaInterceptor;
import com.bintech.metrix.constants.ApiConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 使用 Redis 保存 token 与用户的映射，避免服务重启导致会话失效。
     */
    @Bean
    public SaTokenDao saTokenDao(StringRedisTemplate stringRedisTemplate) {
        return new SaTokenRedisDao(stringRedisTemplate);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns(ApiConstants.PATH_PATTERNS);
    }
}
