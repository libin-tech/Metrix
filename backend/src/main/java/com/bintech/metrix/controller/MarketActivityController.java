package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.service.MarketActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 赚钱效应分析控制器
 */
@RestController
@RequestMapping("/api/market-activity")
@RequiredArgsConstructor
@SaCheckLogin
public class MarketActivityController {

    private final MarketActivityService marketActivityService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getMarketActivity() {
        Map<String, Object> result = marketActivityService.getMarketActivity();
        return ApiResponse.success(result);
    }
}
