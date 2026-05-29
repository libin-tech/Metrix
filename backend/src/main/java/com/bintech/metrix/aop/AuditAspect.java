package com.bintech.metrix.aop;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.annotation.Audit;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 审计日志切面，拦截标注了@Audit注解的Controller方法，
 * 异步记录操作日志到数据库。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Pointcut("@within(com.bintech.metrix.annotation.Audit) || @annotation(com.bintech.metrix.annotation.Audit)")
    public void auditPointcut() {}

    @Around("auditPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
            recordAudit(joinPoint, null);
        } catch (Throwable e) {
            recordAudit(joinPoint, e);
            throw e;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.debug("审计切面耗时: {}ms - {}.{}", elapsed,
                    joinPoint.getSignature().getDeclaringType().getSimpleName(),
                    joinPoint.getSignature().getName());
        }
        return result;
    }

    private void recordAudit(ProceedingJoinPoint joinPoint, Throwable exception) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();

            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            Method method = methodSignature.getMethod();
            Class<?> declaringClass = method.getDeclaringClass();

            Audit classAudit = AnnotationUtils.findAnnotation(declaringClass, Audit.class);
            Audit methodAudit = AnnotationUtils.findAnnotation(method, Audit.class);

            if (classAudit == null && methodAudit == null) {
                return;
            }

            String action = resolveAction(classAudit, methodAudit, method);
            String resourceType = resolveResourceType(classAudit, methodAudit, declaringClass);
            String description = resolveDescription(classAudit, methodAudit);
            String detail = buildDetail(joinPoint, exception, description);

            Long userId = resolveUserId();
            String username = resolveUsername();
            String ip = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            auditLogService.log(userId, username, action, resourceType, null, detail, ip, userAgent);
        } catch (Exception e) {
            log.error("审计日志记录失败", e);
        }
    }

    private Long resolveUserId() {
        try {
            if (StpUtil.isLogin()) {
                return StpUtil.getLoginIdAsLong();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveUsername() {
        try {
            if (StpUtil.isLogin()) {
                return (String) StpUtil.getExtra("username");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveAction(Audit classAudit, Audit methodAudit, Method method) {
        if (methodAudit != null && !methodAudit.action().isEmpty()) {
            return methodAudit.action();
        }
        if (classAudit != null && !classAudit.action().isEmpty()) {
            return classAudit.action();
        }
        return inferActionFromHttpMethod(method);
    }

    private String inferActionFromHttpMethod(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation instanceof GetMapping) {
                return "查询";
            }
            if (annotation instanceof PostMapping) {
                return "新增";
            }
            if (annotation instanceof PutMapping || annotation instanceof PatchMapping) {
                return "修改";
            }
            if (annotation instanceof DeleteMapping) {
                return "删除";
            }
        }
        return "未知";
    }

    private String resolveResourceType(Audit classAudit, Audit methodAudit, Class<?> declaringClass) {
        if (methodAudit != null && !methodAudit.resourceType().isEmpty()) {
            return methodAudit.resourceType();
        }
        if (classAudit != null && !classAudit.resourceType().isEmpty()) {
            return classAudit.resourceType();
        }
        return extractResourceFromRequestMapping(declaringClass);
    }

    private String extractResourceFromRequestMapping(Class<?> declaringClass) {
        RequestMapping rm = AnnotationUtils.findAnnotation(declaringClass, RequestMapping.class);
        if (rm == null || rm.value().length == 0) {
            return declaringClass.getSimpleName();
        }
        String path = rm.value()[0];
        if (path.startsWith(ApiConstants.API_BASE_PATH + "/")) {
            return path.substring((ApiConstants.API_BASE_PATH + "/").length());
        }
        return path;
    }

    private String resolveDescription(Audit classAudit, Audit methodAudit) {
        if (methodAudit != null && !methodAudit.description().isEmpty()) {
            return methodAudit.description();
        }
        if (classAudit != null && !classAudit.description().isEmpty()) {
            return classAudit.description();
        }
        return "";
    }

    private String buildDetail(ProceedingJoinPoint joinPoint, Throwable exception, String description) {
        try {
            Map<String, Object> detailMap = new LinkedHashMap<>();
            if (!description.isEmpty()) {
                detailMap.put("description", description);
            }
            detailMap.put("method", joinPoint.getSignature().getDeclaringType().getSimpleName()
                    + "." + joinPoint.getSignature().getName());

            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                Map<String, Object> params = new LinkedHashMap<>();
                MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
                String[] parameterNames = methodSignature.getParameterNames();
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof HttpServletRequest) {
                        continue;
                    }
                    String name = parameterNames != null && i < parameterNames.length
                            ? parameterNames[i] : "arg" + i;
                    try {
                        params.put(name, JSONUtil.toJsonStr(args[i]));
                    } catch (Exception e) {
                        params.put(name, args[i].toString());
                    }
                }
                if (!params.isEmpty()) {
                    detailMap.put("params", params);
                }
            }

            if (exception != null) {
                detailMap.put("error", exception.getMessage());
            }

            return JSONUtil.toJsonStr(detailMap);
        } catch (Exception e) {
            return exception != null ? "error: " + exception.getMessage() : "";
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
