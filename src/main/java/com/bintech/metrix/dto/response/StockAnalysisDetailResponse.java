package com.bintech.metrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 股票分析详情响应DTO
 * 
 * <p>提供格式化的分析记录详情，支持Markdown格式渲染。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAnalysisDetailResponse {

    /**
     * 分析记录ID
     */
    private Long id;

    /**
     * 股票代码
     */
    private String stockCode;

    /**
     * 股票名称
     */
    private String stockName;

    /**
     * 分析类型
     */
    private String analysisType;

    /**
     * 分析结果（Markdown格式）
     */
    private String analysisResult;

    /**
     * 新闻摘要
     */
    private String newsSummary;

    /**
     * 新闻列表
     */
    private List<NewsItem> newsList;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 分析概览（JSON格式）
     */
    private String analysisOverview;
}