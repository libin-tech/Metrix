package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.StockAnalysisRequest;
import com.bintech.metrix.dto.response.CursorPageResult;
import com.bintech.metrix.dto.response.StockAnalysisDetailResponse;
import com.bintech.metrix.dto.response.StockAnalysisResponse;
import com.bintech.metrix.repository.entity.StockAnalysisRecord;

import java.util.List;

/**
 * 股票分析服务接口
 *
 * <p>提供股票分析的核心业务，包括发起分析、查询记录、导出分析内容等。
 */
public interface StockAnalysisService {

    /**
     * 执行股票分析
     *
     * @param request 分析请求（股票代码、分析维度等）
     * @param record  待保存的分析记录
     * @return 分析结果响应
     */
    StockAnalysisResponse analyzeStock(StockAnalysisRequest request, StockAnalysisRecord record);

    /** 根据ID获取分析记录 */
    StockAnalysisRecord getAnalysisById(Long id);

    /** 获取所有分析记录（按时间倒序） */
    List<StockAnalysisRecord> getAllAnalysisRecords();

    /** 删除指定分析记录 */
    void deleteAnalysisRecord(Long id);

    /** 清理超出上限（默认50条）的旧分析记录 */
    void cleanupExcessRecords();
    
    /**
     * 获取分析记录详情（Markdown格式）
     * 
     * @param id 分析记录ID
     * @return 格式化的分析详情响应
     */
    StockAnalysisDetailResponse getAnalysisDetail(Long id);
    
    /**
     * 游标分页查询分析记录
     *
     * @param cursor 上一页最后一条记录的ID，null或0表示第一页
     * @param limit  每页条数，默认10
     * @return 游标分页结果
     */
    CursorPageResult<StockAnalysisRecord> cursorQuery(Long cursor, int limit);

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