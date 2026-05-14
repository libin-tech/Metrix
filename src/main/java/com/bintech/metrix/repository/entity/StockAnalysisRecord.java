package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bintech.metrix.enums.StockAnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 股票分析记录实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("stock_analysis_record")
public class StockAnalysisRecord extends BaseEntity {

    /**
     * 股票代码
     */
    @TableField(value = "stock_code")
    private String stockCode;

    /**
     * 股票名称
     */
    @TableField(value = "stock_name")
    private String stockName;

    /**
     * 分析类型
     */
    @TableField(value = "analysis_type")
    private String analysisType;

    /**
     * 分析状态：ANALYZING-分析中，COMPLETED-分析完成，FAILED-分析失败
     */
    @TableField(value = "status")
    private StockAnalysisStatus status;

    /**
     * 分析结果
     */
    @TableField(value = "analysis_result")
    private String analysisResult;

    /**
     * 市场数据（JSON格式）
     */
    @TableField(value = "market_data")
    private String marketData;

    /**
     * 市场数据（JSON格式）
     */
    @TableField(value = "depth_data")
    private String depthData;

    /**
     * 市场数据（JSON格式）
     */
    @TableField(value = "klines_data")
    private String klinesData;

    /**
     * 新闻摘要（JSON格式）
     */
    @TableField(value = "news_summary")
    private String newsSummary;

    /**
     * 分析概览（JSON格式）
     */
    @TableField(value = "analysis_overview")
    private String analysisOverview;

}
