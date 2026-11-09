package com.eldercare.backend.health.web.request;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.health.internal.threshold.domain.NewThreshold;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;

/** Thân lệnh Thiết lập ngưỡng, schema {@code NewThresholdVersion} của api-007 (FR-020). */
public record ThresholdRequest(
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"FACILITY", "PERSONAL"})
        String scope,

        Long residentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long indicatorId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant effectiveFrom,
        Reason reason,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ThresholdComponentRequest> components) {

    /** Bốn cận của một thành phần; cận trống là không so phía đó. */
    public record ThresholdComponentRequest(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            String code,

            BigDecimal dangerBelow,
            BigDecimal warningBelow,
            BigDecimal warningAbove,
            BigDecimal dangerAbove) {}

    public NewThreshold toDomain() {
        var bands = new LinkedHashMap<String, ThresholdBand>();
        if (components != null) {
            for (var c : components) {
                bands.put(
                        c.code(),
                        new ThresholdBand(c.dangerBelow(), c.warningBelow(), c.warningAbove(), c.dangerAbove()));
            }
        }
        return new NewThreshold(scope, residentId, indicatorId == null ? 0 : indicatorId, effectiveFrom, bands, reason);
    }
}
