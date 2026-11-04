package com.eldercare.backend.belongings.internal.catalog.domain;

/** Thuộc tính loại đồ (FR-001): có số tiền (tiền mặt), cần đồng ý khi giao sử dụng (FR-015a). */
public record BelongingTypeAttributes(boolean hasCashAmount, boolean requiresConsentForUse) {}
