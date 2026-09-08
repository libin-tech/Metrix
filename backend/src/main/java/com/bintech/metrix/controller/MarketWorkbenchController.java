package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.annotation.CheckConfig;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.MarketWorkbenchResponse;
import com.bintech.metrix.dto.response.PortfolioHoldingVO;
import com.bintech.metrix.enums.ConfigType;
import com.bintech.metrix.service.MarketWorkbenchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 单股行情工作台接口。 */
@RestController
@RequestMapping("/api/market-workbench")
@RequiredArgsConstructor
@SaCheckLogin
public class MarketWorkbenchController {

    private final MarketWorkbenchService marketWorkbenchService;

    @GetMapping("/overview")
    @SaCheckPermission("market:workbench:view")
    @CheckConfig(required = ConfigType.MARKET_DATA)
    public ApiResponse<MarketWorkbenchResponse> overview(@RequestParam String thscode) {
        return ApiResponse.success(marketWorkbenchService.load(thscode, StpUtil.getLoginIdAsLong()));
    }

    @GetMapping("/holdings")
    @SaCheckPermission("market:workbench:holdings")
    public ApiResponse<List<PortfolioHoldingVO>> holdings() {
        return ApiResponse.success(marketWorkbenchService.holdings());
    }

    @GetMapping("/anomalies")
    @SaCheckPermission("market:workbench:anomalies")
    @CheckConfig(required = ConfigType.MARKET_DATA)
    public ApiResponse<Map<String, Object>> anomalies() {
        return ApiResponse.success(marketWorkbenchService.anomalies(StpUtil.getLoginIdAsLong()));
    }
}
