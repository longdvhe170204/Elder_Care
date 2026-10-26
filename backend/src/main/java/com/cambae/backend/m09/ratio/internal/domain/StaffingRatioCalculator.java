package com.cambae.backend.m09.ratio.internal.domain;

import com.cambae.backend.common.Role;
import com.cambae.backend.m01.api.dto.CareLevel;
import java.math.*;
import java.util.*;

/** BR-M09-02/14: tính theo người trong ca; so ngưỡng trước khi làm tròn số hiển thị. */
public final class StaffingRatioCalculator {
    private StaffingRatioCalculator() {}

    public record Resident(long id, CareLevel careLevel) {}

    public record Member(long id, Set<Role> roles, boolean absent, boolean approvedLeave) {}

    public record Result(
            BigDecimal totalWeight, int staffCount, BigDecimal ratio, BigDecimal threshold, boolean meetsThreshold) {}

    public static Result calculate(
            Collection<Resident> residents,
            Collection<Member> members,
            Map<CareLevel, BigDecimal> weights,
            BigDecimal threshold) {
        if (threshold == null || threshold.signum() <= 0)
            throw new IllegalArgumentException("Thiếu ngưỡng dương của mẫu ca");
        BigDecimal total = BigDecimal.ZERO;
        Set<Long> seen = new HashSet<>();
        for (var resident : residents) {
            if (!seen.add(resident.id())) continue;
            var weight = weights.get(resident.careLevel());
            if (weight == null || weight.signum() <= 0)
                throw new IllegalArgumentException("Thiếu trọng số dương của mức chăm sóc");
            total = total.add(weight);
        }
        int count = (int) members.stream()
                .filter(m -> !m.absent()
                        && !m.approvedLeave()
                        && !Collections.disjoint(m.roles(), Set.of(Role.NURSING_STAFF, Role.CAREGIVER)))
                .map(Member::id)
                .distinct()
                .count();
        BigDecimal ratio = count == 0 ? null : total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        boolean passed =
                count == 0 ? total.signum() == 0 : total.compareTo(threshold.multiply(BigDecimal.valueOf(count))) <= 0;
        return new Result(total.setScale(2, RoundingMode.UNNECESSARY), count, ratio, threshold, passed);
    }
}
