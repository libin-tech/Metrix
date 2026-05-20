package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bintech.metrix.dto.request.NewsSourceConfigRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.repository.entity.NewsSourceConfig;
import com.bintech.metrix.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 新闻源配置控制器
 *
 * <p>提供新闻数据源的CRUD管理，支持多种新闻源切换。
 */
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
