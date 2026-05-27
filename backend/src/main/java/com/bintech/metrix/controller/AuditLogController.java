package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@SaCheckLogin
public class AuditLogController {

    private final AuditLogService auditLogService;

    @PostMapping("/log")
    public ApiResponse<Void> logFrontendAction(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        String username = (String) StpUtil.getExtra("username");
        String action = body.getOrDefault("action", "");
        String resourceType = body.getOrDefault("resourceType", "");
        String resourceId = body.getOrDefault("resourceId", "");
        String detail = body.getOrDefault("detail", "");
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        auditLogService.log(userId, username, action, resourceType, resourceId, detail, ip, userAgent);
        return ApiResponse.success(null);
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
