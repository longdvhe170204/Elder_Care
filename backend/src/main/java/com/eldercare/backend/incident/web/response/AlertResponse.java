package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.alert.domain.AlertDetails;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Schema {@code Alert} của api-007. Vị trí GPS, mốc tới nơi, người bấm, loại sự việc, nơi chọn thuộc US12, US13 (T067
 * trở đi) nên lát này để trống. {@code handoverAcknowledged} là dấu "tạm nhận, chờ xác nhận bàn giao" (FR-037).
 */
public record AlertResponse(
        Long id,
        Long residentId,
        String type,
        String typeKey,
        IncidentLevel level,
        Integer occurrenceCount,
        Instant firstAt,
        Instant latestAt,
        AlertStatus status,
        Integer escalationLevel,
        Boolean maxEscalationReached,
        Instant acknowledgementDeadline,
        String assignee,
        Boolean handoverAcknowledged,
        Boolean noOnDutyDoctor,
        Long incidentId,
        Long criticalFlagId,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY) Boolean merged,

        @Schema(allowableValues = {"SYSTEM", "MANUAL", "SOS", "ASSIST_BUTTON"})
        String origin,

        @Schema(types = {"string", "null"}) String pressedBy,

        @Schema(types = {"string", "null"}) String eventType,

        @Schema(types = {"string", "null"}) String chosenPlace,

        @Schema(description = "LocationView hoặc null; vị trí GPS nối ở US12, US13")
        Object latestLocation,

        @Schema(
                types = {"string", "null"},
                format = "date-time")
        Instant arrivedAt,

        Boolean skipArrivalTime,
        List<AlertSource> sources,
        List<String> allowedCommands) {

    private static final String STAFF_ASSIST = "STAFF_ASSIST_REQUEST";
    private static final String RESIDENT_HELP = "RESIDENT_HELP_REQUEST";

    public record AlertSource(
            String sourceType, Long sourceId, IncidentLevel level, Instant occurredAt, Instant resolvedAt) {}

    public static AlertResponse of(AlertDetails details, boolean merged) {
        AlertRecord alert = details.alert();
        return new AlertResponse(
                alert.id(),
                alert.residentId(),
                alert.type(),
                alert.typeLock(),
                alert.level(),
                alert.occurrenceCount(),
                alert.firstAt(),
                alert.latestAt(),
                alert.status(),
                alert.escalationLevel(),
                alert.maxEscalationAt() != null,
                alert.acknowledgementDeadline(),
                details.assigneeName(),
                alert.handoverAcknowledged(),
                alert.noOnDutyDoctor(),
                alert.incidentId(),
                alert.criticalFlagId(),
                merged,
                origin(alert),
                details.press().pressedBy(),
                details.press().eventType(),
                details.press().chosenPlace(),
                null,
                alert.arrivedAt(),
                alert.skipArrivalTime(),
                details.sources().stream()
                        .map(source -> new AlertSource(
                                source.sourceType(),
                                source.sourceId(),
                                source.level() == null ? null : IncidentLevel.valueOf(source.level()),
                                source.occurredAt(),
                                source.resolvedAt()))
                        .toList(),
                details.allowedCommands().stream().sorted().toList());
    }

    /** Nguồn tạo (FR-026): (d) nút SOS, (e) nút hỗ trợ khẩn cấp, còn lại thủ công hoặc hệ thống. */
    private static String origin(AlertRecord alert) {
        if (RESIDENT_HELP.equals(alert.type())) return "SOS";
        if (STAFF_ASSIST.equals(alert.type())) return "ASSIST_BUTTON";
        return alert.createdById() != null ? "MANUAL" : "SYSTEM";
    }
}
