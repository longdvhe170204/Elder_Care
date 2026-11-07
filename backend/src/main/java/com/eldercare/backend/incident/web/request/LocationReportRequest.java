package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.incident.internal.alert.domain.AlertLocationReport;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/** Thân {@code POST /sos/{id}/location}, {@code /emergency-assist/{id}/location} ({@code LocationReport}; FR-047h). */
@Schema(name = "LocationReport")
public record LocationReportRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "-90", maximum = "90")
        BigDecimal latitude,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "-180", maximum = "180")
        BigDecimal longitude,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0")
        BigDecimal accuracyMeters,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant measuredAt) {

    public AlertLocationReport toReport() {
        return new AlertLocationReport(latitude, longitude, accuracyMeters, measuredAt);
    }
}
