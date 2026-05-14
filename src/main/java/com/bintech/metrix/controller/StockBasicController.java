package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.PageResult;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.StockBasicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/stock-basic")
@RequiredArgsConstructor
@SaCheckLogin
public class StockBasicController {

    private final StockBasicService stockBasicService;

    @GetMapping("/page")
    public ApiResponse<PageResult<StockBasic>> pageQuery(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<StockBasic> result = stockBasicService.pageQuery(keyword, page, size);
        return ApiResponse.success(result);
    }

    @PostMapping("/import")
    public ApiResponse<String> importCsv(@RequestParam("file") MultipartFile file) {
        String msg = stockBasicService.importCsv(file);
        return ApiResponse.success(msg);
    }
}
