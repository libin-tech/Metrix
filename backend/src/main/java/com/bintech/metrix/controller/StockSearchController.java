package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.StockInfo;
import com.bintech.metrix.service.StockSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 股票搜索控制器
 *
 * <p>提供股票关键字搜索能力，支持代码和名称的模糊匹配，用于前端自动补全。
 */
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@SaCheckLogin
public class StockSearchController {

    private final StockSearchService stockSearchService;

    @GetMapping("/search")
    @SaCheckPermission("stock:search")
    public ApiResponse<List<StockInfo>> searchStocks(@RequestParam String keyword) {
        List<StockInfo> stocks = stockSearchService.searchStocks(keyword);
        return ApiResponse.success(stocks);
    }
}
