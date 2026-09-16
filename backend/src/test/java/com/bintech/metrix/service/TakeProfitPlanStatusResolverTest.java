package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.TakeProfitPlanVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TakeProfitPlanStatusResolverTest {

    @Test
    void marksSecondTargetAsTrailingWhenTrailingDrawdownIsConfigured() {
        TakeProfitPlanVO plan = enabledPlan();
        plan.setTrailingDrawdownPercent(new BigDecimal("8"));

        TakeProfitPlanStatusResolver.resolve(plan, new BigDecimal("12.500"), LocalDate.of(2026, 9, 15));

        assertEquals("TRACKING", plan.getStatus());
        assertNull(plan.getNextTargetPrice());
    }

    @Test
    void marksPlanExpiredBeforeCheckingPriceTarget() {
        TakeProfitPlanVO plan = enabledPlan();
        plan.setExpiresOn(LocalDate.of(2026, 9, 14));

        TakeProfitPlanStatusResolver.resolve(plan, new BigDecimal("12.500"), LocalDate.of(2026, 9, 15));

        assertEquals("EXPIRED", plan.getStatus());
    }

    @Test
    void triggersTrailingStopAfterPriceFallsFromRecordedPeak() {
        TakeProfitPlanVO plan = enabledPlan();
        plan.setTrailingDrawdownPercent(new BigDecimal("8"));
        plan.setTrailingPeakPrice(new BigDecimal("13.000"));

        TakeProfitPlanStatusResolver.resolve(plan, new BigDecimal("11.960"), LocalDate.of(2026, 9, 15));

        assertEquals("TRAILING_STOP_TRIGGERED", plan.getStatus());
        assertEquals(true, plan.getTrailingStopTriggered());

        TakeProfitPlanStatusResolver.resolve(plan, new BigDecimal("12.700"), LocalDate.of(2026, 9, 15));

        assertEquals("TRAILING_STOP_TRIGGERED", plan.getStatus());
    }

    private TakeProfitPlanVO enabledPlan() {
        TakeProfitPlanVO plan = new TakeProfitPlanVO();
        plan.setEnabled(true);
        plan.setFirstTargetPrice(new BigDecimal("11.000"));
        plan.setSecondTargetPrice(new BigDecimal("12.000"));
        return plan;
    }
}
