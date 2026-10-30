package com.bintech.metrix.core.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

import static com.bintech.metrix.constants.MarketIntelligenceConstants.*;

/** 缺失值保持缺失，避免 NaN、无穷及零分母变成正常财务数值。 */
public final class IntelligenceNumbers {
    private IntelligenceNumbers() {}

    public static Double number(Object value) {
        if (value == null) return null;
        try {
            double parsed = Double.parseDouble(value.toString());
            return Double.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    public static Double positive(Object value) {
        Double parsed = number(value);
        return parsed != null && parsed > 0 ? parsed : null;
    }

    public static Integer count(Object value) {
        Double parsed = number(value);
        return parsed != null && parsed >= 0 && parsed <= Integer.MAX_VALUE && parsed == Math.rint(parsed)
                ? parsed.intValue() : null;
    }

    public static Double percent(Double numerator, Double denominator) {
        return numerator == null || denominator == null || denominator <= 0 ? null : round(numerator / denominator * 100);
    }

    public static Double change(Double current, Double previous) {
        return current == null || previous == null ? null : percent(current - previous, previous);
    }

    public static double round(double value) {
        return BigDecimal.valueOf(value).setScale(MONEY_SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    public static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        return numerator == null || denominator == null || denominator.signum() <= 0 ? null
                : numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public static <T> Double average(List<T> values, int period, Function<T, Double> extract) {
        if (values.size() < period) return null;
        List<Double> sample = values.subList(values.size() - period, values.size()).stream().map(extract).toList();
        if (sample.stream().anyMatch(value -> value == null || !Double.isFinite(value))) return null;
        return sample.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }
}
