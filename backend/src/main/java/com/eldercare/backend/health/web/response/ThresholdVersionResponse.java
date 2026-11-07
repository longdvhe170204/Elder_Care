package com.eldercare.backend.health.web.response;

import com.eldercare.backend.health.internal.threshold.domain.ThresholdListing;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Phiên bản ngưỡng trả qua HTTP, schema {@code ThresholdVersion} của api-007; {@code setBy} là mã tài khoản. */
public record ThresholdVersionResponse(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"FACILITY", "PERSONAL"})
        String scope,

        Long residentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long indicatorId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant effectiveFrom,
        String reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ThresholdComponentResponse> components,
        Long id,

        @Schema(allowableValues = {"PENDING", "EFFECTIVE", "EXPIRED", "CANCELLED"})
        String status,

        Instant effectiveUntil,
        String setBy) {

    public record ThresholdComponentResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String code,

            BigDecimal dangerBelow,
            BigDecimal warningBelow,
            BigDecimal warningAbove,
            BigDecimal dangerAbove) {}

    public static ThresholdVersionResponse from(ThresholdListing listing) {
        var v = listing.version();
        return new ThresholdVersionResponse(
                v.scope(),
                v.residentId(),
                v.indicatorId(),
                v.effectiveFrom(),
                listing.reason(),
                v.components().entrySet().stream()
                        .sorted(java.util.Map.Entry.comparingByKey())
                        .map(e -> new ThresholdComponentResponse(
                                e.getKey(),
                                e.getValue().lowerDangerous(),
                                e.getValue().lowerWarning(),
                                e.getValue().upperWarning(),
                                e.getValue().upperDangerous()))
                        .toList(),
                v.id(),
                v.status(),
                v.effectiveUntil(),
                Long.toString(listing.createdBy()));
    }
}
