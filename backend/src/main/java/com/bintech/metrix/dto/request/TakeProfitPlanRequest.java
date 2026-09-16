package com.bintech.metrix.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 止盈计划配置请求。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TakeProfitPlanRequest {

    private Boolean enabled;

    @DecimalMin(value = "0.001", message = "第一目标价必须大于0")
    private BigDecimal firstTargetPrice;

    @DecimalMin(value = "0", message = "第一档减仓比例不能小于0")
    @DecimalMax(value = "100", message = "第一档减仓比例不能超过100")
    private BigDecimal firstTargetRatio;

    @DecimalMin(value = "0.001", message = "第二目标价必须大于0")
    private BigDecimal secondTargetPrice;

    @DecimalMin(value = "0", message = "第二档减仓比例不能小于0")
    @DecimalMax(value = "100", message = "第二档减仓比例不能超过100")
    private BigDecimal secondTargetRatio;

    @DecimalMin(value = "0.1", message = "跟踪回撤比例必须大于0")
    @DecimalMax(value = "100", message = "跟踪回撤比例不能超过100")
    private BigDecimal trailingDrawdownPercent;

    private LocalDate expiresOn;

    /** 校验前两档止盈累计减仓比例。 */
    public boolean hasValidTargetRatio() {
        BigDecimal firstRatio = firstTargetRatio == null ? BigDecimal.ZERO : firstTargetRatio;
        BigDecimal secondRatio = secondTargetRatio == null ? BigDecimal.ZERO : secondTargetRatio;
        return firstRatio.add(secondRatio).compareTo(BigDecimal.valueOf(100)) <= 0;
    }
}
