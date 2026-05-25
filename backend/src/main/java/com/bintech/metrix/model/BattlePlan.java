package com.bintech.metrix.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 作战计划模块
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattlePlan {

    /**
     * 理想入场位价格
     */
    private BigDecimal idealEntryPrice;

    /**
     * 理想入场位描述
     */
    private String idealEntryDesc;

    /**
     * 次优入场位价格
     */
    private BigDecimal suboptimalEntryPrice;

    /**
     * 次优入场位描述
     */
    private String suboptimalEntryDesc;

    /**
     * 止损位价格
     */
    private BigDecimal stopLossPrice;

    /**
     * 止损位描述
     */
    private String stopLossDesc;

    /**
     * 目标位价格
     */
    private BigDecimal targetPrice;

    /**
     * 目标位描述
     */
    private String targetDesc;

    /**
     * 风险回报比
     */
    private BigDecimal riskRewardRatio;
}