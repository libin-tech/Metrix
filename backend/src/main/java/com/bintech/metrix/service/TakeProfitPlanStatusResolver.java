package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.TakeProfitPlanVO;
import com.bintech.metrix.enums.TakeProfitPlanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 将止盈计划与当前价格转换为可展示的提醒状态。 */
public final class TakeProfitPlanStatusResolver {

    private TakeProfitPlanStatusResolver() {
    }

    /**
     * 根据当前价格刷新计划提醒状态，不改变用户配置的目标价与比例。
     */
    public static void resolve(TakeProfitPlanVO plan, BigDecimal currentPrice, LocalDate today) {
        if (!Boolean.TRUE.equals(plan.getEnabled())) {
            plan.setStatus(TakeProfitPlanStatus.DISABLED.name());
            return;
        }
        if (Boolean.TRUE.equals(plan.getTrailingStopTriggered())) {
            plan.setStatus(TakeProfitPlanStatus.TRAILING_STOP_TRIGGERED.name());
            plan.setNextTargetPrice(null);
            return;
        }
        if (isExpired(plan.getExpiresOn(), today)) {
            plan.setStatus(TakeProfitPlanStatus.EXPIRED.name());
            return;
        }
        if (currentPrice == null) {
            plan.setStatus(TakeProfitPlanStatus.WAITING.name());
            plan.setNextTargetPrice(plan.getFirstTargetPrice());
            return;
        }
        if (plan.getTrailingPeakPrice() != null || isReached(currentPrice, plan.getSecondTargetPrice())) {
            resolveSecondTargetStatus(plan, currentPrice);
            return;
        }
        if (isReached(currentPrice, plan.getFirstTargetPrice())) {
            plan.setStatus(TakeProfitPlanStatus.FIRST_TARGET_REACHED.name());
            plan.setNextTargetPrice(plan.getSecondTargetPrice());
            return;
        }
        plan.setStatus(TakeProfitPlanStatus.WAITING.name());
        plan.setNextTargetPrice(plan.getFirstTargetPrice());
    }

    private static boolean isExpired(LocalDate expiresOn, LocalDate today) {
        return expiresOn != null && expiresOn.isBefore(today);
    }

    private static boolean isReached(BigDecimal currentPrice, BigDecimal targetPrice) {
        return targetPrice != null && currentPrice.compareTo(targetPrice) >= 0;
    }

    private static void resolveSecondTargetStatus(TakeProfitPlanVO plan, BigDecimal currentPrice) {
        if (plan.getTrailingDrawdownPercent() == null) {
            plan.setStatus(TakeProfitPlanStatus.SECOND_TARGET_REACHED.name());
            plan.setNextTargetPrice(null);
            return;
        }
        BigDecimal peakPrice = maxPrice(plan.getTrailingPeakPrice(), currentPrice);
        plan.setTrailingPeakPrice(peakPrice);
        if (hasReachedTrailingStop(currentPrice, peakPrice, plan.getTrailingDrawdownPercent())) {
            plan.setTrailingStopTriggered(true);
            plan.setStatus(TakeProfitPlanStatus.TRAILING_STOP_TRIGGERED.name());
            plan.setNextTargetPrice(null);
            return;
        }
        plan.setStatus(TakeProfitPlanStatus.TRACKING.name());
        plan.setNextTargetPrice(null);
    }

    private static BigDecimal maxPrice(BigDecimal firstPrice, BigDecimal secondPrice) {
        if (firstPrice == null || secondPrice.compareTo(firstPrice) > 0) {
            return secondPrice;
        }
        return firstPrice;
    }

    private static boolean hasReachedTrailingStop(BigDecimal currentPrice, BigDecimal peakPrice, BigDecimal drawdownPercent) {
        BigDecimal trailingStopPrice = peakPrice.multiply(BigDecimal.ONE
                .subtract(drawdownPercent.divide(BigDecimal.valueOf(100))));
        return currentPrice.compareTo(peakPrice) < 0 && currentPrice.compareTo(trailingStopPrice) <= 0;
    }
}
