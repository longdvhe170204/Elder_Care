package com.eldercare.backend.belongings.web.request;

/** Thân {@code BelongingType} (api-013): {@code reason} bắt buộc khi đổi dấu "cần đồng ý khi giao sử dụng". */
public record BelongingTypeRequest(
        String name, Boolean hasCashAmount, Boolean requiresConsentForUse, String status, String reason) {}
