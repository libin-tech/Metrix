package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.PageResult;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.StockBasicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.dto.response.StockSyncResult;

/**
 * 股票基础信息控制器
 *
 * <p>提供股票基础数据的分页查询和增量同步功能。
 */
@RestController
@RequestMapping("/api/stock-basic")
@RequiredArgsConstructor
@SaCheckLogin
public class StockBasicController {

    private final StockBasicService stockBasicService;

    @GetMapping("/page")
    @SaCheckPermission("stock:basic:page")
    public ApiResponse<PageResult<StockBasic>> pageQuery(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = ApiConstants.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = ApiConstants.DEFAULT_PAGE_SIZE) int size) {
        PageResult<StockBasic> result = stockBasicService.pageQuery(keyword, page, size);
        return ApiResponse.success(result);
    }

    @PostMapping("/sync")
    // 迁移期间沿用原标的维护权限，旧导入接口已移除。
    @SaCheckPermission(value = {"stock:basic:sync", "stock:basic:import"}, mode = SaMode.OR)
    public ApiResponse<StockSyncResult> sync() {
        return ApiResponse.success(stockBasicService.sync(StpUtil.getLoginIdAsLong()));
    }
}
