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
        @Schema(defaultValue = "false") Boolean createDespiteOpen,
        @Schema(
                        defaultValue = "false",
                        description =
                                "Ghi nhận sau: việc đã xảy ra và đã xử lý xong, chỉ lưu hồ sơ; không báo khẩn, không leo thang")
                Boolean recordedLater,
        @Schema(description = "Báo người nhà: INFORMED (ai, lúc nào, cách nào) hoặc NOT_YET") FamilyNoticeBody familyNotice,
        @Schema(
                        defaultValue = "false",
                        description = "Chỉ với recordedLater: ghi xong đóng luôn (Điều dưỡng, Trưởng tầng, Bác sĩ), bắt buộc resolution")
                Boolean closeNow,
        @Schema(description = "Kết quả xử lý khi closeNow") String resolution) {

    public record FamilyNoticeBody(String state, Instant at, String method, String note) {}

    /** Chữ ký cũ (trước BF-06 ghi nhận sau) cho các nơi dựng thân lệnh bằng mã. */
    public RecordIncidentRequest(
            Long residentId,
            Long incidentTypeId,
            String context,
            Instant occurredAt,
            Instant discoveredAt,
            Long floorAreaId,
            String location,
            String ongoingActivity,
            String description,
            IncidentLevel level,
            String lowerLevelReason,
            String initialResponse,
            Boolean createDespiteOpen) {
        this(
                residentId,
                incidentTypeId,
                context,
                occurredAt,
                discoveredAt,
                floorAreaId,
                location,
                ongoingActivity,
                description,
                level,
                lowerLevelReason,
                initialResponse,
                createDespiteOpen,
                null,
                null,
                null,
                null);
    }

    /** Chữ ký 15 đối số (trước closeNow): giữ cho các nơi dựng thân lệnh bằng mã. */
    public RecordIncidentRequest(
            Long residentId,
            Long incidentTypeId,
            String context,
            Instant occurredAt,
            Instant discoveredAt,
            Long floorAreaId,
            String location,
            String ongoingActivity,
            String description,
            IncidentLevel level,
            String lowerLevelReason,
            String initialResponse,
            Boolean createDespiteOpen,
            Boolean recordedLater,
            FamilyNoticeBody familyNotice) {
        this(
                residentId,
                incidentTypeId,
                context,
                occurredAt,
                discoveredAt,
                floorAreaId,
                location,
                ongoingActivity,
                description,
                level,
                lowerLevelReason,
                initialResponse,
                createDespiteOpen,
                recordedLater,
                familyNotice,
                null,
                null);
    }
}
