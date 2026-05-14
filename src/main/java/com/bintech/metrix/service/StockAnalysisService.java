package com.bintech.metrix.service;

import java.util.List;

import com.bintech.metrix.dto.request.StockAnalysisRequest;
import com.bintech.metrix.dto.response.StockAnalysisDetailResponse;
import com.bintech.metrix.dto.response.StockAnalysisResponse;
import com.bintech.metrix.repository.entity.StockAnalysisRecord;

public interface StockAnalysisService {
    StockAnalysisResponse analyzeStock(StockAnalysisRequest request, StockAnalysisRecord record);
    StockAnalysisRecord getAnalysisById(Long id);
    List<StockAnalysisRecord> getAllAnalysisRecords();
    void deleteAnalysisRecord(Long id);
    void cleanupExcessRecords();
    
    /**
     * 获取分析记录详情（Markdown格式）
     * 
     * @param id 分析记录ID
     * @return 格式化的分析详情响应
     */
    StockAnalysisDetailResponse getAnalysisDetail(Long id);
    
    /**
     * 获取分析结果的Markdown格式内容
     * 
     * @param id 分析记录ID
     * @return Markdown格式的分析结果
     */
    String getAnalysisResultAsMarkdown(Long id);
    
    /**
     * 执行异步分析任务
     * 
     * @param recordId 记录ID
     * @param request 分析请求
     */
    void executeAnalysis(Long recordId, StockAnalysisRequest request);

    /**
     * 推送分析记录到飞书
     *
     * @param id 分析记录ID
     */
    void pushToFeishu(Long id);
}