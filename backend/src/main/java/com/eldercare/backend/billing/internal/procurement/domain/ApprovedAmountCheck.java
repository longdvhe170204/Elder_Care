package com.eldercare.backend.billing.internal.procurement.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 019 research W-05 (KiemTraVuot; FR-008a, BR-M11-18): số thực tế không vượt số được duyệt quá CFG-M11-07 phần trăm. */
public final class ApprovedAmountCheck {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private ApprovedAmountCheck() {}

    /** Số tiền thực tế lớn nhất được ghi, làm tròn xuống tới đồng. */
    public static long maximum(long approved, BigDecimal percent) {
        return BigDecimal.valueOf(approved)
                .multiply(HUNDRED.add(percent))
                .divide(HUNDRED, 0, RoundingMode.FLOOR)
                .longValueExact();
    }

    public static boolean withinLimit(long approved, long actual, BigDecimal percent) {
        return actual <= maximum(approved, percent);
    }
}
