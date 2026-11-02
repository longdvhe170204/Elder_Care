package com.cambae.backend.m11.pricing.internal.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** FR-009/BR-M11-09, Q-395: ngày nội trú tính đủ giá, kể cả ngày vắng khi chưa giải phóng giường. */
public final class ResidencyDayFee {
    private ResidencyDayFee() {}

    public static BigDecimal base(BigDecimal monthlyPrice, LocalDate date) {
        int days = date.lengthOfMonth();
        var ordinary = monthlyPrice.divide(BigDecimal.valueOf(days), 0, RoundingMode.HALF_UP);
        return date.getDayOfMonth() == days
                ? monthlyPrice.subtract(ordinary.multiply(BigDecimal.valueOf(days - 1)))
                : ordinary;
    }

    public static BigDecimal monthly(BigDecimal monthlyPrice, LocalDate date) {
        return direct(base(monthlyPrice, date));
    }

    public static BigDecimal direct(BigDecimal dailyPrice) {
        return DongRounding.round(dailyPrice);
    }
}
