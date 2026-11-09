package com.eldercare.backend.billing.internal.period.domain;

import com.eldercare.backend.billing.api.dto.CostType;
import java.math.*;
import java.util.*;

/** FR-032: caller chỉ truyền tổng so sánh của hai bảng thường đủ tháng. */
public final class PeriodCostChange {
    private PeriodCostChange() {}

    public record Warning(BigDecimal increaseRatio, List<String> topThreeIncreases) {}

    public static Optional<Warning> compare(
            Map<CostType, Long> current,
            Map<CostType, Long> previous,
            boolean fullMonths,
            BigDecimal thresholdPercent) {
        if (!fullMonths) return Optional.empty();
        BigDecimal before = sum(previous), after = sum(current);
        if (before.signum() < 0 || after.compareTo(before) <= 0) return Optional.empty();
        BigDecimal increase = after.subtract(before).multiply(BigDecimal.valueOf(100));
        if (increase.compareTo(before.multiply(thresholdPercent)) <= 0) return Optional.empty();
        var top = Arrays.stream(CostType.values())
                .filter(t -> current.getOrDefault(t, 0L) > previous.getOrDefault(t, 0L))
                .sorted(Comparator.<CostType>comparingLong(
                                t -> current.getOrDefault(t, 0L) - previous.getOrDefault(t, 0L))
                        .reversed()
                        .thenComparing(Enum::name))
                .limit(3)
                .map(Enum::name)
                .toList();
        return Optional.of(
                new Warning(before.signum() == 0 ? null : increase.divide(before, 6, RoundingMode.HALF_UP), top));
    }

    private static BigDecimal sum(Map<CostType, Long> totals) {
        return totals.values().stream().map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
