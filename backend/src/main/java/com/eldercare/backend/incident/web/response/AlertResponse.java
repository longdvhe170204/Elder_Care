package com.eldercare.backend.incident.web.response;

import com.eldercare.backend.incident.internal.alert.domain.AlertDetails;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Schema {@code Alert} của api-007; vị trí GPS, mốc tới nơi, người bấm, loại sự việc, nơi chọn của cảnh báo từ nút SOS,
 * nút hỗ trợ (US12, US13). {@code handoverAcknowledged} là dấu "tạm nhận, chờ xác nhận bàn giao" (FR-037).
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

        @Schema(
                types = {"string", "null"},
                description = "Nơi người bấm nút hỗ trợ gõ tự do")
        String placeNote,

        @Schema(
                types = {"string", "null"},
                description = "Mô tả lúc tạo (cảnh báo thủ công) hoặc của nguồn")
        String description,

        @Schema(
                types = {"string", "null"},
                description = "Tên tầng của cảnh báo")
        String floorName,

        @Schema(description = "Các lần báo sau đã gộp vào cảnh báo thủ công (FR-030)")
        List<AlertReport> reports,

        @Schema(description = "LocationView hoặc null (FR-047h)")
        Object latestLocation,

        @Schema(
                types = {"string", "null"},
                format = "date-time")
        Instant arrivedAt,

        Boolean skipArrivalTime,
        List<AlertSource> sources,
        List<String> allowedCommands,

        @Schema(
                types = {"integer", "null"},
                description = "Tầng của cảnh báo; rỗng là khu vực chung hoặc không theo tầng (đề xuất 2026-11-23)")
        Long floorAreaId,

        @Schema(
                types = {"string", "null"},
                description = "Họ tên người đóng (đề xuất 2026-11-23)")
        String closedBy,

        @Schema(
                types = {"string", "null"},
                description = "Kết quả xử lý khi đóng (đề xuất 2026-11-23)")
        String resolution,

        @Schema(description = "Người xem là người gửi yêu cầu hỗ trợ này (đề xuất 2026-11-23)")
        Boolean pressedByMe,

        @Schema(description = "Người gửi yêu cầu hỗ trợ không trong ca lúc bấm (đề xuất 2026-11-23)")
        Boolean senderOffShift,

        @Schema(description = "Người đã bấm Tôi đến giúp (đề xuất 2026-11-23)")
        List<String> helpers,

        @Schema(description = "Tin người gửi nhắn thêm (đề xuất 2026-11-23)")
        List<SenderMessage> senderMessages,

        List<AlertFollowUp> followUps) {

    private static final String STAFF_ASSIST = "STAFF_ASSIST_REQUEST";
    private static final String RESIDENT_HELP = "RESIDENT_HELP_REQUEST";

    public record AlertReport(String reportedBy, String description, Instant at) {}

    public record SenderMessage(String by, String text, Instant at) {}

    public record AlertSource(
            String sourceType, Long sourceId, IncidentLevel level, Instant occurredAt, Instant resolvedAt) {}

    /** Kết quả theo dõi (FR-087, Q-443). */
    public record AlertFollowUp(String content, Instant at, String recordedBy) {}

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
                details.press().placeNote(),
                alert.description(),
                details.floorName(),
                details.reports().stream()
                        .map(report -> new AlertReport(report.reportedBy(), report.description(), report.at()))
                        .toList(),
                LocationResponse.of(details.press().latestLocation()),
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
                details.allowedCommands().stream().sorted().toList(),
                alert.floorAreaId(),
                details.closedByName(),
                alert.resolution(),
                details.assist().mine(),
                details.assist().senderOffShift(),
                details.assist().helpers(),
                details.assist().messages().stream()
                        .map(message -> new SenderMessage(message.by(), message.text(), message.at()))
                        .toList(),
                details.followUps().stream()
                        .map(f -> new AlertFollowUp(f.content(), f.at(), f.recordedBy()))
                        .toList());
    }

    /** Nguồn tạo (FR-026): (d) nút SOS, (e) nút hỗ trợ khẩn cấp, còn lại thủ công hoặc hệ thống. */
    private static String origin(AlertRecord alert) {
        if (RESIDENT_HELP.equals(alert.type())) return "SOS";
        if (STAFF_ASSIST.equals(alert.type())) return "ASSIST_BUTTON";
        return alert.createdById() != null ? "MANUAL" : "SYSTEM";
    }
}
