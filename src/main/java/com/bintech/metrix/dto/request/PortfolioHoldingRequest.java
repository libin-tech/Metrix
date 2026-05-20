package com.bintech.metrix.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioHoldingRequest {

    @NotNull(message = "账户不能为空")
    private Long accountId;

    @NotBlank(message = "标的代码不能为空")
    private String stockCode;

    @NotBlank(message = "标的名称不能为空")
    private String stockName;

    private BigDecimal cost;

    private BigDecimal quantity;
}
