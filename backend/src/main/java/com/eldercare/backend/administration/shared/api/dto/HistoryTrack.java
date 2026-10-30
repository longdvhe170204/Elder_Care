package com.eldercare.backend.administration.shared.api.dto;

import java.util.Objects;

/**
 * Một dòng lịch sử của {@code status_history}: thuộc tính {@code attribute} của loại đối tượng {@code entityType} (có tiền
 * tố module, ví dụ {@code M01.RESIDENT}); {@code details} là record của cột {@code details} ({@code Void} khi không có).
 */
public record HistoryTrack<D>(String entityType, String attribute, Class<D> details) {

    public HistoryTrack {
        Objects.requireNonNull(details, "details");
        // Tiền tố module kiểm như mã loại
        SharedKind.of(entityType, Void.class);
        if (attribute == null || attribute.isBlank() || attribute.length() > 40) {
            throw new IllegalArgumentException("attribute: " + attribute);
        }
    }

    public String module() {
        return entityType.substring(0, entityType.indexOf('.'));
    }
}
