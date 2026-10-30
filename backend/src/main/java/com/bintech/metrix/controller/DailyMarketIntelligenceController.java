package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.DailyMarketIntelligence.Result;
import com.bintech.metrix.service.DailyMarketIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SaCheckLogin
@RequiredArgsConstructor
@RequestMapping("/api/market-intelligence")
public class DailyMarketIntelligenceController {
    private final DailyMarketIntelligenceService service;

    @GetMapping
    public ApiResponse<Result> getReport() {
        return ApiResponse.success(service.getReport(StpUtil.getLoginIdAsLong()));
    }
}
