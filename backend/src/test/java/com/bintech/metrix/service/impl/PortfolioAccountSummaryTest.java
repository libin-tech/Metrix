package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.dto.response.PortfolioAccountSummary;
import com.bintech.metrix.dto.response.PortfolioHoldingListResponse;
import com.bintech.metrix.repository.dao.BrokerAccountDao;
import com.bintech.metrix.repository.dao.PortfolioHoldingDao;
import com.bintech.metrix.repository.entity.BrokerAccount;
import com.bintech.metrix.repository.entity.PortfolioHolding;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class PortfolioAccountSummaryTest {

    private static final long USER_ID = 42L;
    private final PortfolioHoldingDao holdingDao = mock(PortfolioHoldingDao.class);
    private final BrokerAccountDao accountDao = mock(BrokerAccountDao.class);

    @Test
    void includesEveryAccountAndHoldingBeyondTheWorkbenchDisplayLimit() {
        when(accountDao.selectByUserId(USER_ID)).thenReturn(List.of(
                account(1L, "2000.00", "-120.35"), account(2L, "1000.00", "30.25")));
        when(holdingDao.selectByUserId(USER_ID)).thenReturn(IntStream.range(0, 9)
                .mapToObj(index -> holding(index == 8 ? 2L : 1L))
                .toList());

        PortfolioAccountSummary summary = withPortfolioService(PortfolioHoldingServiceImpl::getAccountSummary);

        assertThat(summary.totalPrincipal()).isEqualByComparingTo("3000.00");
        assertThat(summary.totalMarketValue()).isEqualByComparingTo("1080.00");
        assertThat(summary.totalProfitLoss()).isEqualByComparingTo("180.00");
        assertThat(summary.realizedProfitLoss()).isEqualByComparingTo("-90.10");
        assertThat(summary.cumulativeProfitLoss()).isEqualByComparingTo("89.90");
        assertThat(summary.totalAssets()).isEqualByComparingTo(summary.totalMarketValue());
    }

    @Test
    void includesRealizedLossAfterAllPositionsAreClosed() {
        when(accountDao.selectByUserId(USER_ID)).thenReturn(List.of(account(1L, "1000.00", "-250.50")));
        when(holdingDao.selectByUserId(USER_ID)).thenReturn(List.of());

        PortfolioAccountSummary summary = withPortfolioService(PortfolioHoldingServiceImpl::getAccountSummary);

        assertThat(summary.totalPrincipal()).isEqualByComparingTo("1000.00");
        assertThat(summary.totalMarketValue()).isZero();
        assertThat(summary.totalProfitLoss()).isZero();
        assertThat(summary.realizedProfitLoss()).isEqualByComparingTo("-250.50");
        assertThat(summary.cumulativeProfitLoss()).isEqualByComparingTo("-250.50");
        assertThat(summary.totalAssets()).isZero();
    }

    @Test
    void limitsAccountSummaryToTheSelectedAccountWhileStockSearchFiltersTheList() {
        when(accountDao.selectByUserId(USER_ID)).thenReturn(List.of(
                account(1L, "2000.00", "-120.35"), account(2L, "1000.00", "30.25")));
        when(holdingDao.selectByUserId(USER_ID)).thenReturn(List.of(holding(1L), holding(2L)));

        PortfolioHoldingListResponse result = withPortfolioService(service -> service.getHoldings("unmatched", 2L));

        assertThat(result.getHoldings()).isEmpty();
        assertThat(result.getSummary().getTotalMarketValue()).isZero();
        PortfolioAccountSummary summary = result.getAccountSummary();
        assertThat(summary.totalPrincipal()).isEqualByComparingTo("1000.00");
        assertThat(summary.totalMarketValue()).isEqualByComparingTo("120.00");
        assertThat(summary.totalAssets()).isEqualByComparingTo(summary.totalMarketValue());
        assertThat(summary.totalProfitLoss()).isEqualByComparingTo("20.00");
        assertThat(summary.realizedProfitLoss()).isEqualByComparingTo("30.25");
        assertThat(summary.cumulativeProfitLoss()).isEqualByComparingTo("50.25");
    }

    private <T> T withPortfolioService(Function<PortfolioHoldingServiceImpl, T> call) {
        var service = new PortfolioHoldingServiceImpl(holdingDao, accountDao, null, null, null);
        try (var login = mockStatic(StpUtil.class)) {
            login.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);
            return call.apply(service);
        } finally {
            service.shutdown();
        }
    }

    private BrokerAccount account(long id, String principal, String realizedProfitLoss) {
        BrokerAccount account = new BrokerAccount();
        account.setId(id);
        account.setTotalPrincipal(new BigDecimal(principal));
        account.setRealizedProfitLoss(new BigDecimal(realizedProfitLoss));
        return account;
    }

    private PortfolioHolding holding(long accountId) {
        PortfolioHolding holding = new PortfolioHolding();
        holding.setAccountId(accountId);
        holding.setCost(new BigDecimal("10.00"));
        holding.setQuantity(BigDecimal.TEN);
        holding.setCachedPrice(new BigDecimal("12.00"));
        return holding;
    }
}
