package com.bintech.metrix.dto.response;

import java.math.BigDecimal;

/** 当前用户所选券商账户的资金汇总。 */
public record PortfolioAccountSummary(
        BigDecimal totalPrincipal,
        BigDecimal totalAssets,
        BigDecimal totalMarketValue,
        BigDecimal totalProfitLoss,
        BigDecimal realizedProfitLoss,
        BigDecimal cumulativeProfitLoss) {
}
