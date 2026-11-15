package com.eldercare.backend.billing.internal.source.domain;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Dữ liệu chỉ mục của nguồn tính phí; unit là khóa đơn vị tính phí, không phải đơn vị hiển thị. {@code usageUnit} là đơn vị
 * dùng nguồn gửi (thuốc), rỗng khi nguồn không gửi.
 */
public record IndexedBillingSource(
        String sourceType,
        String sourceId,
        String unit,
        long residentId,
        LocalDate incurredOn,
        Instant occurredAt,
        Long serviceId,
        Long medicationId,
        BigDecimal quantity,
        String usageUnit) {

    public IndexedBillingSource(
            String sourceType,
            String sourceId,
            String unit,
            long residentId,
            LocalDate incurredOn,
            Instant occurredAt,
            Long serviceId,
            Long medicationId,
            BigDecimal quantity) {
        this(sourceType, sourceId, unit, residentId, incurredOn, occurredAt, serviceId, medicationId, quantity, null);
    }

    public IndexedBillingSource {
        Objects.requireNonNull(sourceType);
        Objects.requireNonNull(sourceId);
        Objects.requireNonNull(unit);
        Objects.requireNonNull(incurredOn);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(quantity);
        if (quantity.stripTrailingZeros().scale() > 3
                || quantity.abs().compareTo(new BigDecimal("999999999.999")) > 0) {
            throw new IllegalArgumentException("Số lượng nguồn vượt độ chính xác DECIMAL(12,3)");
        }
    }

    /** Dấu xử lý có độ dài cố định; ID và đơn vị nguồn vẫn lưu nguyên văn trong chỉ mục. */
    public String processingMarker(String eventKey) {
        Objects.requireNonNull(eventKey);
        String key = sourceType + "\u0000" + sourceId + "\u0000" + unit + "\u0000" + eventKey;
        try {
            return "BILLED:"
                    + HexFormat.of()
                            .formatHex(
                                    MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK thiếu SHA-256", e);
        }
    }
}
