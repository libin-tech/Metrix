package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.service.MarketInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 首页市场洞察控制器。
 */
@RestController
@RequestMapping("/api/market-insights")
@RequiredArgsConstructor
@SaCheckLogin
public class MarketInsightController {

    private final MarketInsightService marketInsightService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getMarketInsights() {
        return ApiResponse.success(marketInsightService.getMarketInsights());
    }
}
