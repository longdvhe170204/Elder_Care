package com.cambae.backend.m11.pricing.internal.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** FR-013: làm tròn nửa lên theo giá trị tuyệt đối, kết quả là số nguyên đồng. */
public final class DongRounding {
    private DongRounding() {}

    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }
}
