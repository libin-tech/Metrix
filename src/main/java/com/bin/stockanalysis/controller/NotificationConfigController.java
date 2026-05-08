package com.bin.stockanalysis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bin.stockanalysis.dto.request.NotificationConfigRequest;
import com.bin.stockanalysis.dto.response.ApiResponse;
import com.bin.stockanalysis.repository.entity.NotificationConfig;
import com.bin.stockanalysis.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
@SaCheckLogin
public class NotificationConfigController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationConfig>> getAllConfigs() {
        List<NotificationConfig> configs = notificationService.getAllConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/active")
    public ApiResponse<List<NotificationConfig>> getActiveConfigs() {
        List<NotificationConfig> configs = notificationService.getActiveConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/{id}")
    public ApiResponse<NotificationConfig> getConfigById(@PathVariable Long id) {
        NotificationConfig config = notificationService.getConfigById(id);
        return ApiResponse.success(config);
    }

    @PostMapping
    public ApiResponse<NotificationConfig> createConfig(@Valid @RequestBody NotificationConfigRequest request) {
        NotificationConfig config = notificationService.createConfig(request);
        return ApiResponse.success("Notification config created successfully", config);
    }

    @PutMapping("/{id}")
    public ApiResponse<NotificationConfig> updateConfig(@PathVariable Long id, @Valid @RequestBody NotificationConfigRequest request) {
        NotificationConfig config = notificationService.updateConfig(id, request);
        return ApiResponse.success("Notification config updated successfully", config);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        notificationService.deleteConfig(id);
        return ApiResponse.success("Notification config deleted successfully", null);
    }

    @PostMapping("/test")
    public ApiResponse<Boolean> testNotification(@RequestParam String title, @RequestParam String content) {
        boolean result = notificationService.sendFeishuNotification(title, content);
        return ApiResponse.success("Notification test result", result);
    }
}
