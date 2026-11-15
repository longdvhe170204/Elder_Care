package com.eldercare.backend.care.internal.result.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Lượng tính phí của kết quả (FR-051; quyết định 05/11, {@code specs/010-…/care-quantity-decision-2026-11-05.md}): vật
 * phẩm ({@code ITEM_QUANTITY}) là số lượng thực đã ghi, dịch vụ theo lần ({@code PERFORMED}) là 1. Loại kết quả khác (ml,
 * mức ăn, vận động, giá trị đo…) không tự quy đổi sang đơn vị dịch vụ: rỗng, lệnh chặn tới khi có đơn vị, kỳ tính của
 * dịch vụ theo {@code serviceId}.
 */
public final class CareBillableQuantity {
    private CareBillableQuantity() {}

    public static Optional<BigDecimal> of(String resultType, BigDecimal numericValue) {
        return switch (resultType) {
            case "PERFORMED" -> Optional.of(BigDecimal.ONE);
            case "ITEM_QUANTITY" -> Optional.ofNullable(numericValue);
            default -> Optional.empty();
        };
    }

    public static boolean supported(String resultType) {
        return resultType.equals("PERFORMED") || resultType.equals("ITEM_QUANTITY");
    }

    /**
     * Kỳ tính của dịch vụ khớp loại kết quả (quyết định 05/11 mục 1, 2): dịch vụ theo lần/buổi ({@code OCCURRENCE},
     * {@code SESSION}) mới phát 1; vật phẩm phát lượng thực khi dịch vụ tính theo đơn vị ({@code UNIT}). Không mặc định 1
     * khi không khớp.
     */
    public static boolean matches(String resultType, String billingPeriod) {
        return switch (resultType) {
            case "PERFORMED" -> billingPeriod.equals("OCCURRENCE") || billingPeriod.equals("SESSION");
            case "ITEM_QUANTITY" -> billingPeriod.equals("UNIT");
            default -> false;
        };
    }
}
