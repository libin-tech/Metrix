package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.bintech.metrix.dto.request.AiModelConfigRequest;
import com.bintech.metrix.dto.request.AiModelTestRequest;
import com.bintech.metrix.dto.response.AiModelTestResponse;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.repository.entity.AiModelConfig;
import com.bintech.metrix.service.AiModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI模型配置控制器
 *
 * <p>提供AI模型的CRUD管理、连接测试、激活切换等功能。
 */
@RestController
@RequestMapping("/api/ai-model")
@RequiredArgsConstructor
@SaCheckLogin
public class AiModelConfigController {

    private final AiModelService aiModelService;

    @GetMapping
    @SaCheckPermission("config:ai-model:list")
    public ApiResponse<List<AiModelConfig>> getAllConfigs() {
        List<AiModelConfig> configs = aiModelService.getAllConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/active")
    @SaCheckPermission("config:ai-model:active")
    public ApiResponse<List<AiModelConfig>> getActiveConfigs() {
        List<AiModelConfig> configs = aiModelService.getActiveConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/{id}")
    @SaCheckPermission("config:ai-model:detail")
    public ApiResponse<AiModelConfig> getConfigById(@PathVariable Long id) {
        AiModelConfig config = aiModelService.getConfigById(id);
        return ApiResponse.success(config);
    }

    @PostMapping
    @SaCheckPermission("config:ai-model:create")
    public ApiResponse<AiModelConfig> createConfig(@Valid @RequestBody AiModelConfigRequest request) {
        AiModelConfig config = aiModelService.createConfig(request);
        return ApiResponse.success("AI Model config created successfully", config);
    }

    @PutMapping("/{id}")
    @SaCheckPermission("config:ai-model:update")
    public ApiResponse<AiModelConfig> updateConfig(@PathVariable Long id, @Valid @RequestBody AiModelConfigRequest request) {
        AiModelConfig config = aiModelService.updateConfig(id, request);
        return ApiResponse.success("AI Model config updated successfully", config);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("config:ai-model:delete")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        aiModelService.deleteConfig(id);
        return ApiResponse.success("AI Model config deleted successfully", null);
    }

    @PostMapping("/test")
    @SaCheckPermission("config:ai-model:test")
    public ApiResponse<AiModelTestResponse> testModel(@Valid @RequestBody AiModelTestRequest request) {
        AiModelTestResponse result = aiModelService.testConnection(request);
        return ApiResponse.success(result);
    }
}
