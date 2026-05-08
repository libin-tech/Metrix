package com.bin.stockanalysis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bin.stockanalysis.dto.request.MarketDataConfigRequest;
import com.bin.stockanalysis.dto.response.ApiResponse;
import com.bin.stockanalysis.repository.entity.MarketDataConfig;
import com.bin.stockanalysis.service.MarketDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
@SaCheckLogin
public class MarketDataConfigController {

    private final MarketDataService marketDataService;

    @GetMapping
    public ApiResponse<List<MarketDataConfig>> getAllConfigs() {
        List<MarketDataConfig> configs = marketDataService.getAllConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/active")
    public ApiResponse<List<MarketDataConfig>> getActiveConfigs() {
        List<MarketDataConfig> configs = marketDataService.getActiveConfigs();
        return ApiResponse.success(configs);
    }

    @GetMapping("/{id}")
    public ApiResponse<MarketDataConfig> getConfigById(@PathVariable Long id) {
        MarketDataConfig config = marketDataService.getConfigById(id);
        return ApiResponse.success(config);
    }

    @PostMapping
    public ApiResponse<MarketDataConfig> createConfig(@Valid @RequestBody MarketDataConfigRequest request) {
        MarketDataConfig config = marketDataService.createConfig(request);
        return ApiResponse.success("Market data config created successfully", config);
    }

    @PutMapping("/{id}")
    public ApiResponse<MarketDataConfig> updateConfig(@PathVariable Long id, @Valid @RequestBody MarketDataConfigRequest request) {
        MarketDataConfig config = marketDataService.updateConfig(id, request);
        return ApiResponse.success("Market data config updated successfully", config);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        marketDataService.deleteConfig(id);
        return ApiResponse.success("Market data config deleted successfully", null);
    }

    @GetMapping("/stock/{stockCode}")
    public ApiResponse<Map<String, Object>> fetchMarketData(@PathVariable String stockCode) {
        Map<String, Object> result = marketDataService.fetchMarketData(stockCode);
        return ApiResponse.success(result);
    }

    @GetMapping("/stock/{stockCode}/realtime")
    public ApiResponse<Map<String, Object>> fetchRealTimeData(@PathVariable String stockCode) {
        Map<String, Object> result = marketDataService.fetchRealTimeData(stockCode);
        return ApiResponse.success(result);
    }
}
