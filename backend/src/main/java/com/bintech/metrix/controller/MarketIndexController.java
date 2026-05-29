package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bintech.metrix.annotation.Audit;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.service.MarketIndexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Audit(resourceType = "大盘行情")
@RestController
@RequestMapping("/api/market-index")
@RequiredArgsConstructor
@SaCheckLogin
public class MarketIndexController {

    private final MarketIndexService marketIndexService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getMarketIndex() {
        Map<String, Object> result = marketIndexService.getMarketIndex();
        return ApiResponse.success(result);
    }
}
