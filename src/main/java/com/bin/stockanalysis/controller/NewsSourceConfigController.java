package com.bin.stockanalysis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bin.stockanalysis.dto.request.NewsSourceConfigRequest;
import com.bin.stockanalysis.dto.response.ApiResponse;
import com.bin.stockanalysis.repository.entity.NewsSourceConfig;
import com.bin.stockanalysis.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news-source")
@RequiredArgsConstructor
@SaCheckLogin
public class NewsSourceConfigController {

    private final NewsService newsService;

    @GetMapping
    public ApiResponse<List<NewsSourceConfig>> getAllConfigs() {
        List<NewsSourceConfig> configs = newsService.getAllConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/active")
    public ApiResponse<List<NewsSourceConfig>> getActiveConfigs() {
        List<NewsSourceConfig> configs = newsService.getActiveConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/{id}")
    public ApiResponse<NewsSourceConfig> getConfigById(@PathVariable Long id) {
        NewsSourceConfig config = newsService.getConfigById(id);
        return ApiResponse.success(config);
    }

    @PostMapping
    public ApiResponse<NewsSourceConfig> createConfig(@Valid @RequestBody NewsSourceConfigRequest request) {
        NewsSourceConfig config = newsService.createConfig(request);
        return ApiResponse.success("News source config created successfully", config);
    }

    @PutMapping("/{id}")
    public ApiResponse<NewsSourceConfig> updateConfig(@PathVariable Long id, @Valid @RequestBody NewsSourceConfigRequest request) {
        NewsSourceConfig config = newsService.updateConfig(id, request);
        return ApiResponse.success("News source config updated successfully", config);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        newsService.deleteConfig(id);
        return ApiResponse.success("News source config deleted successfully", null);
    }


}
