package com.bintech.metrix.config;

import com.bintech.metrix.constants.BusinessConstants;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 安全认证配置类
 * 配置登录验证拦截器及排除路径
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 添加拦截器配置
     * 对 API 请求进行登录验证，排除特定的无需认证路径
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    StpUtil.checkLogin();
                }))
                .addPathPatterns("/api/**")
                .excludePathPatterns(BusinessConstants.EXCLUDED_AUTH_PATHS);
    }
}
