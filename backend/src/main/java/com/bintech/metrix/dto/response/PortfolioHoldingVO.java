package com.bintech.metrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioHoldingVO {
    private Long id;
    private Long accountId;
    private String brokerName;
    private String accountNumber;
    private String stockCode;
    private String stockName;
    private BigDecimal cost;
    private BigDecimal quantity;
    private BigDecimal currentPrice;
    private BigDecimal profitLossPercent;
    private BigDecimal profitLossAmount;
}
