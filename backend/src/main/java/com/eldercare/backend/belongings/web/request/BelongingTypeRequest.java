package com.eldercare.backend.belongings.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân {@code BelongingType} (api-013): {@code reason} bắt buộc khi đổi dấu "cần đồng ý khi giao sử dụng". */
public record BelongingTypeRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        Boolean hasCashAmount,
        Boolean requiresConsentForUse,
        String status,
        String reason) {}
