package com.bin.stockanalysis.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
     * 分析结果
     */
    @TableField(value = "analysis_result")
    private String analysisResult;

    /**
     * 置信度分数
     */
    @TableField(value = "confidence_score")
    private BigDecimal confidenceScore;

    /**
     * 市场数据（JSON格式）
     */
    @TableField(value = "market_data")
    private String marketData;

    /**
     * 新闻摘要（JSON格式）
     */
    @TableField(value = "news_summary")
    private String newsSummary;
}
