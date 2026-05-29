package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.bintech.metrix.annotation.Audit;
import com.bintech.metrix.dto.request.MarketDataConfigRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.repository.entity.MarketDataConfig;
import com.bintech.metrix.service.MarketDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行情源配置控制器
 *
 * <p>提供行情数据源的CRUD管理，支持多种数据源配置。
 */
@Audit(resourceType = "行情源配置")
@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
@SaCheckLogin
public class MarketDataConfigController {

    private final MarketDataService marketDataService;

    @GetMapping
    @SaCheckPermission("config:market-data:list")
    public ApiResponse<List<MarketDataConfig>> getAllConfigs() {
        List<MarketDataConfig> configs = marketDataService.getAllConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/active")
    @SaCheckPermission("config:market-data:active")
    public ApiResponse<List<MarketDataConfig>> getActiveConfigs() {
        List<MarketDataConfig> configs = marketDataService.getActiveConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/{id}")
    @SaCheckPermission("config:market-data:detail")
    public ApiResponse<MarketDataConfig> getConfigById(@PathVariable Long id) {
        MarketDataConfig config = marketDataService.getConfigById(id);
        return ApiResponse.success(config);
    }

    @PostMapping
    @SaCheckPermission("config:market-data:create")
    public ApiResponse<MarketDataConfig> createConfig(@Valid @RequestBody MarketDataConfigRequest request) {
        MarketDataConfig config = marketDataService.createConfig(request);
        return ApiResponse.success("Market data config created successfully", config);
    }

    @PutMapping("/{id}")
    @SaCheckPermission("config:market-data:update")
    public ApiResponse<MarketDataConfig> updateConfig(@PathVariable Long id, @Valid @RequestBody MarketDataConfigRequest request) {
        MarketDataConfig config = marketDataService.updateConfig(id, request);
        return ApiResponse.success("Market data config updated successfully", config);
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("config:market-data:delete")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        marketDataService.deleteConfig(id);
        return ApiResponse.success("Market data config deleted successfully", null);
    }

}
