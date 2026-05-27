package com.bintech.metrix.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import com.bintech.metrix.constants.ApiConstants;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns(ApiConstants.PATH_PATTERNS);
    }
}
