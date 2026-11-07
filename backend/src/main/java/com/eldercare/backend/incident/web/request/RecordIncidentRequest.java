package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Thân lệnh ghi nhận sự cố, schema {@code RecordIncident} của api-007 (FR-040). */
public record RecordIncidentRequest(
        Long residentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long incidentTypeId,

        @Schema(
                allowableValues = {
                    "CARE",
                    "EATING",
                    "MEDICATION",
                    "MOBILITY",
                    "ACTIVITY",
                    "TRANSPORT",
                    "TREATMENT",
                    "OFFSITE_ACTIVITY",
                    "WRONG_MEAL_SERVED",
                    "BELONGINGS"
                })
        String context,

        Instant occurredAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant discoveredAt,
        Long floorAreaId,
        String location,
        String ongoingActivity,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) IncidentLevel level,
        String lowerLevelReason,
        String initialResponse,
        @Schema(defaultValue = "false") Boolean createDespiteOpen) {}
