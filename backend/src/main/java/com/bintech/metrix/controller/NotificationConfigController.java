package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.bintech.metrix.dto.request.NotificationConfigRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.repository.entity.NotificationConfig;
import com.bintech.metrix.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通知配置控制器
 *
 * <p>提供飞书等通知渠道的配置管理功能。
 */
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@SaCheckLogin
public class NotificationConfigController {

    private final NotificationService notificationService;

    @GetMapping
    @SaCheckPermission("config:notification:list")
    public ApiResponse<List<NotificationConfig>> getAllConfigs() {
        List<NotificationConfig> configs = notificationService.getAllConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/active")
    @SaCheckPermission("config:notification:active")
    public ApiResponse<List<NotificationConfig>> getActiveConfigs() {
        List<NotificationConfig> configs = notificationService.getActiveConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/{id}")
    @SaCheckPermission("config:notification:detail")
    public ApiResponse<NotificationConfig> getConfigById(@PathVariable Long id) {
        NotificationConfig config = notificationService.getConfigById(id);
        return ApiResponse.success(config);
    }

    @PostMapping
    @SaCheckPermission("config:notification:create")
    public ApiResponse<NotificationConfig> createConfig(@Valid @RequestBody NotificationConfigRequest request) {
        NotificationConfig config = notificationService.createConfig(request);
        return ApiResponse.success("Notification config created successfully", config);
    }

    @PutMapping("/{id}")
    @SaCheckPermission("config:notification:update")
    public ApiResponse<NotificationConfig> updateConfig(@PathVariable Long id, @Valid @RequestBody NotificationConfigRequest request) {
        NotificationConfig config = notificationService.updateConfig(id, request);
        return ApiResponse.success("Notification config updated successfully", config);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("config:notification:delete")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        notificationService.deleteConfig(id);
        return ApiResponse.success("Notification config deleted successfully", null);
    }

    @PostMapping("/test")
    @SaCheckPermission("config:notification:test")
    public ApiResponse<Boolean> testNotification(@RequestParam String title, @RequestParam String content) {
        boolean result = notificationService.sendFeishuNotification(title, content);
        return ApiResponse.success("Notification test result", result);
    }
}
