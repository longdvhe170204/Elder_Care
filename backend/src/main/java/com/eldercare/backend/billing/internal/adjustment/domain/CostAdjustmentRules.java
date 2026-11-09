package com.eldercare.backend.billing.internal.adjustment.domain;

import java.math.BigDecimal;

/** FR-026: tính cả khoản điều chỉnh chưa chốt, bỏ khoản đã hủy ở truy vấn dưới khóa. */
public final class CostAdjustmentRules {
    private CostAdjustmentRules() {}

    public static void requireNonnegative(BigDecimal original, BigDecimal adjustments, BigDecimal amount) {
        if (amount == null
                || amount.stripTrailingZeros().scale() > 0
                || amount.abs().precision() > 15)
            throw new IllegalArgumentException("Số tiền điều chỉnh phải là số nguyên đồng trong giới hạn 15 chữ số");
        if (original == null || original.add(adjustments).add(amount).signum() < 0)
            throw new IllegalArgumentException("Tổng khoản gốc và các điều chỉnh không được âm hoặc thiếu đơn giá");
    }
}
