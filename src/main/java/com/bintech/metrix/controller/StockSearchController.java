package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.StockInfo;
import com.bintech.metrix.service.StockSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@SaCheckLogin
public class StockSearchController {

    private final StockSearchService stockSearchService;

    @GetMapping("/search")
    public ApiResponse<List<StockInfo>> searchStocks(@RequestParam String keyword) {
        List<StockInfo> stocks = stockSearchService.searchStocks(keyword);
        return ApiResponse.success(stocks);
    }
}
