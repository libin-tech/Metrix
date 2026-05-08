package com.bin.stockanalysis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bin.stockanalysis.dto.request.AiModelConfigRequest;
import com.bin.stockanalysis.dto.request.AiModelTestRequest;
import com.bin.stockanalysis.dto.response.AiModelTestResponse;
import com.bin.stockanalysis.dto.response.ApiResponse;
import com.bin.stockanalysis.repository.entity.AiModelConfig;
import com.bin.stockanalysis.service.AiModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-model")
@RequiredArgsConstructor
@SaCheckLogin
public class AiModelConfigController {

    private final AiModelService aiModelService;

    @GetMapping
    public ApiResponse<List<AiModelConfig>> getAllConfigs() {
        List<AiModelConfig> configs = aiModelService.getAllConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/active")
    public ApiResponse<List<AiModelConfig>> getActiveConfigs() {
        List<AiModelConfig> configs = aiModelService.getActiveConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/{id}")
    public ApiResponse<AiModelConfig> getConfigById(@PathVariable Long id) {
        AiModelConfig config = aiModelService.getConfigById(id);
        return ApiResponse.success(config);
    }

    @PostMapping
    public ApiResponse<AiModelConfig> createConfig(@Valid @RequestBody AiModelConfigRequest request) {
        AiModelConfig config = aiModelService.createConfig(request);
        return ApiResponse.success("AI Model config created successfully", config);
    }

    @PutMapping("/{id}")
    public ApiResponse<AiModelConfig> updateConfig(@PathVariable Long id, @Valid @RequestBody AiModelConfigRequest request) {
        AiModelConfig config = aiModelService.updateConfig(id, request);
        return ApiResponse.success("AI Model config updated successfully", config);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        aiModelService.deleteConfig(id);
        return ApiResponse.success("AI Model config deleted successfully", null);
    }

    @PostMapping("/test")
    public ApiResponse<AiModelTestResponse> testModel(@Valid @RequestBody AiModelTestRequest request) {
        AiModelTestResponse result = aiModelService.testConnection(request);
        return ApiResponse.success(result);
    }
}
