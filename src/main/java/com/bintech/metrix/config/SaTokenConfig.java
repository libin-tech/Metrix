package com.bintech.metrix.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.constants.BusinessConstants;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        registry.addInterceptor(new SaInterceptor() {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                        // 【核心修复】如果是异步分发（ASYNC），直接放行
                        // 异步分发阶段线程上下文已丢失，但鉴权已在 REQUEST 阶段完成
                        if (request.getDispatcherType() == DispatcherType.ASYNC) {
                            return true;
                        }

                        StpUtil.checkLogin();
                        return true;
                    }
                })
                .addPathPatterns("/api/**")
                .excludePathPatterns(BusinessConstants.EXCLUDED_AUTH_PATHS);
    }
}
