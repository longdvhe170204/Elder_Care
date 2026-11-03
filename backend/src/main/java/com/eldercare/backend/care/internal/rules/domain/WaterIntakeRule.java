package com.eldercare.backend.care.internal.rules.domain;

import java.math.BigDecimal;

/** FR039: phút hiện diện, làm tròn xuống ml; bằng ngưỡng không phải thiếu. */
public final class WaterIntakeRule {
    private WaterIntakeRule() {}

    public static int adjustedTarget(int dailyTarget, long presentMinutes, long elapsedMinutes) {
        if (dailyTarget < 0 || elapsedMinutes <= 0 || presentMinutes < 0 || presentMinutes > elapsedMinutes)
            throw new IllegalArgumentException("Khoảng hiện diện không hợp lệ");
        return Math.toIntExact(Math.multiplyExact((long) dailyTarget, presentMinutes) / elapsedMinutes);
    }

    public static boolean isLow(BigDecimal total, int adjustedTarget, BigDecimal percent) {
        return total.multiply(BigDecimal.valueOf(100))
                        .compareTo(BigDecimal.valueOf(adjustedTarget).multiply(percent))
                < 0;
    }
}
