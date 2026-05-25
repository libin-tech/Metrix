package com.bintech.metrix.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 数据透视模块
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataPivot {

    /**
     * 当前价
     */
    private BigDecimal currentPrice;

    /**
     * 5日均线
     */
    private BigDecimal ma5;

    /**
     * 20日均线
     */
    private BigDecimal ma20;

    /**
     * 60日均线
     */
    private BigDecimal ma60;

    /**
     * 支撑位
     */
    private BigDecimal supportLevel;

    /**
     * 压力位
     */
    private BigDecimal resistanceLevel;

    /**
     * 当前成交量（股）
     */
    private Long volume;

    /**
     * 筹码集中度
     */
    private BigDecimal chipConcentration;

    /**
     * 筹码分布描述
     */
    private String chipDistribution;

    /**
     * 获利盘比例
     */
    private BigDecimal profitRatio;

    /**
     * 套牢盘比例
     */
    private BigDecimal lossRatio;

    /**
     * 平均成本
     */
    private BigDecimal avgCostPrice;

    /**
     * 90%筹码成本区间-低
     */
    private BigDecimal cost90Low;

    /**
     * 90%筹码成本区间-高
     */
    private BigDecimal cost90High;

    /**
     * 90%筹码集中度
     */
    private BigDecimal concentration90;

    /**
     * 70%筹码成本区间-低
     */
    private BigDecimal cost70Low;

    /**
     * 70%筹码成本区间-高
     */
    private BigDecimal cost70High;

    /**
     * 70%筹码集中度
     */
    private BigDecimal concentration70;

    /**
     * 筹码综合总结
     */
    private String chipSummary;

    /**
     * MACD DIF 值（快线）
     */
    private BigDecimal macdDif;

    /**
     * MACD DEA 值（慢线/信号线）
     */
    private BigDecimal macdDea;

    /**
     * MACD 柱状图值（BAR）
     */
    private BigDecimal macdBar;

    /**
     * MACD 信号描述
     */
    private String macdSignal;
}