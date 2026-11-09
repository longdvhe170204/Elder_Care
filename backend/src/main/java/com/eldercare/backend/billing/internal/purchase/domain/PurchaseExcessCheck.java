package com.eldercare.backend.billing.internal.purchase.domain;

import java.math.BigDecimal;

/** T015/FR-024: kiểm mức vượt; ngoại lệ mua gấp còn cần căn cứ danh mục và người yêu cầu ở T027. */
public final class PurchaseExcessCheck {
    private PurchaseExcessCheck() {}

    public enum Result {
        WITHIN_CONSENT,
        EXCESS,
        ADDITIONAL_CONSENT_REQUIRED
    }

    public static Result evaluate(long actual, long allowed, long consentThreshold, BigDecimal excessPercent) {
        if (actual < 0 || allowed < 0 || consentThreshold < 0 || excessPercent.signum() < 0)
            throw new IllegalArgumentException("Số tiền và ngưỡng không được âm");
        if (actual <= allowed) return Result.WITHIN_CONSENT;
        if (actual <= consentThreshold
                || BigDecimal.valueOf(actual - allowed)
                                .multiply(BigDecimal.valueOf(100))
                                .compareTo(BigDecimal.valueOf(allowed).multiply(excessPercent))
                        <= 0) return Result.EXCESS;
        return Result.ADDITIONAL_CONSENT_REQUIRED;
    }
}
