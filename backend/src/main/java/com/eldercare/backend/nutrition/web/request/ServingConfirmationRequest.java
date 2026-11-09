package com.eldercare.backend.nutrition.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Thân lệnh xác nhận phục vụ (api-011 {@code ServingConfirmation}; FR-049 → FR-051): {@code residentId} là người đang được
 * phục vụ; {@code hasEaten}, {@code incident} chỉ gửi sau khi bị chặn {@code PORTION_HAS_ALLERGEN}; {@code deviceTime} khi ghi
 * ngoại tuyến.
 */
public record ServingConfirmationRequest(
        @NotNull Long residentId, Boolean hasEaten, @Valid Incident incident, Instant deviceTime) {

    public record Incident(String severity, String symptoms, String treatment) {}
}
