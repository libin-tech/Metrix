package com.bintech.metrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 持仓止盈计划及其当前提醒状态。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TakeProfitPlanVO {

    private Boolean enabled;
    private BigDecimal firstTargetPrice;
    private BigDecimal firstTargetRatio;
    private BigDecimal secondTargetPrice;
    private BigDecimal secondTargetRatio;
    private BigDecimal trailingDrawdownPercent;
    private BigDecimal trailingPeakPrice;
    private Boolean trailingStopTriggered;
    private LocalDate expiresOn;
    private String status;
    private BigDecimal nextTargetPrice;
}
