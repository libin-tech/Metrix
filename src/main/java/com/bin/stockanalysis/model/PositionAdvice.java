package com.bin.stockanalysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 仓位建议模块
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionAdvice {

    /**
     * 建仓策略描述
     */
    private String positionStrategy;

    /**
     * 建议仓位比例（0-100）
     */
    private BigDecimal positionRatio;

    /**
     * 分阶段建仓计划
     */
    private String stagedPlan;

    /**
     * 风控策略描述
     */
    private String riskControlStrategy;

    /**
     * 资金管理规则
     */
    private String capitalManagementRule;

    /**
     * 风险控制措施
     */
    private String riskControlMeasures;
}