package com.cambae.backend.m11.pricing.internal.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** FR-009: nguồn đưa số buổi có lịch đã loại ngày khu nghỉ và xác định buổi có lịch cuối tháng. */
public final class DayCareSessionFee {
    private DayCareSessionFee() {}

    public static BigDecimal base(BigDecimal monthlyPrice, int scheduledSessionCount, boolean lastScheduledSession) {
        if (scheduledSessionCount <= 0) throw new IllegalArgumentException("Số buổi có lịch phải lớn hơn không");
        var ordinary = monthlyPrice.divide(BigDecimal.valueOf(scheduledSessionCount), 0, RoundingMode.HALF_UP);
        return lastScheduledSession
                ? monthlyPrice.subtract(ordinary.multiply(BigDecimal.valueOf(scheduledSessionCount - 1)))
                : ordinary;
    }

    public static BigDecimal monthly(
            BigDecimal monthlyPrice,
            int scheduledSessionCount,
            boolean lastScheduledSession,
            BigDecimal factorPercent) {
        return direct(base(monthlyPrice, scheduledSessionCount, lastScheduledSession), factorPercent);
    }

    public static BigDecimal direct(BigDecimal sessionPrice, BigDecimal factorPercent) {
        return DongRounding.round(sessionPrice.multiply(factorPercent).movePointLeft(2));
    }
}
