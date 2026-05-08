package com.bin.stockanalysis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bin.stockanalysis.dto.request.StockAnalysisRequest;
import com.bin.stockanalysis.dto.response.ApiResponse;
import com.bin.stockanalysis.dto.response.StockAnalysisResponse;
import com.bin.stockanalysis.repository.entity.StockAnalysisRecord;
import com.bin.stockanalysis.service.StockAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 股票分析控制器
 * 
 * <p>提供股票分析相关的REST API接口，包括：
 * <ul>
 *   <li>分析记录管理：创建、查询、删除分析记录</li>
 *   <li>仪表盘分析：获取多维度分析数据</li>
 *   <li>核心结论：获取综合评分和投资建议</li>
 *   <li>各维度分析数据：技术面、基本面、资金流、舆情等</li>
 * </ul>
 * 
 * <p>所有接口均需要登录认证（@SaCheckLogin）。
 */
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
@SaCheckLogin
public class StockAnalysisController {

    private final StockAnalysisService stockAnalysisService;

    /**
     * 执行股票分析
     * 
     * <p>根据请求参数执行股票分析，支持多种分析类型。
     * 分析结果将保存到数据库并返回给客户端。
     * 
     * @param request 分析请求参数
     * @return 分析响应
     */
    @PostMapping
    public ApiResponse<StockAnalysisResponse> analyzeStock(@Valid @RequestBody StockAnalysisRequest request) {
        StockAnalysisResponse response = stockAnalysisService.analyzeStock(request);
        return ApiResponse.success("Analysis completed successfully", response);
    }

    /**
     * 获取所有分析记录
     * 
     * @return 分析记录列表
     */
    @GetMapping
    public ApiResponse<List<StockAnalysisRecord>> getAllAnalysisRecords() {
        List<StockAnalysisRecord> records = stockAnalysisService.getAllAnalysisRecords();
        return ApiResponse.success(records);
    }

    /**
     * 根据ID获取分析记录
     * 
     * @param id 分析记录ID
     * @return 分析记录
     */
    @GetMapping("/{id}")
    public ApiResponse<StockAnalysisRecord> getAnalysisById(@PathVariable Long id) {
        StockAnalysisRecord record = stockAnalysisService.getAnalysisById(id);
        return ApiResponse.success(record);
    }


}