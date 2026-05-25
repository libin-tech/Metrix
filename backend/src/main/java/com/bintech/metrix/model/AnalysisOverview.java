package com.bintech.metrix.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分析概览主结构类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisOverview {

    /** 股票实时行情模块 */
    private RealTimeMarket realTimeMarket;

    /** 数据透视模块 */
    private DataPivot dataPivot;

    /** 作战计划模块 */
    private BattlePlan battlePlan;

    /** 核心洞察 - 从完整分析报告生成的300字左右简短概述 */
    private String coreInsight;

    /** 十大流通股东数据（JSON字符串） */
    private String topFreeShareholdersData;

    /** 十大流通股东分析 */
    private String topFreeShareholdersAnalysis;
}