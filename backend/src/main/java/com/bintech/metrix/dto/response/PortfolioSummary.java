package com.bintech.metrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioSummary {
    private BigDecimal totalMarketValue;
    private BigDecimal totalProfitLossPercent;
    private BigDecimal totalProfitLossAmount;
    private LocalDateTime refreshTime;
}
