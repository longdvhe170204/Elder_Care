package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/** Nội dung lệnh ghi sự cố, dịch từ GhiNhanSuCo (FR-040). */
public record RecordIncidentCommand(
        Long residentId,
        long incidentTypeId,
        String source,
        Instant occurredAt,
        Instant detectedAt,
        Long floorAreaId,
        String location,
        String ongoingActivity,
        String description,
        String level,
        String lowerLevelReason,
        String initialResponse,
        boolean createDespiteExisting,
        boolean recordedLater,
        FamilyNotice familyNotice) {

    /** Giữ nguyên chữ ký cũ cho mọi nơi gọi hiện có (belongings, nutrition, chăm sóc nguy kịch, chuyến đi): không "ghi nhận sau". */
    public RecordIncidentCommand(
            Long residentId,
            long incidentTypeId,
            String source,
            Instant occurredAt,
            Instant detectedAt,
            Long floorAreaId,
            String location,
            String ongoingActivity,
            String description,
            String level,
            String lowerLevelReason,
            String initialResponse,
            boolean createDespiteExisting) {
        this(
                residentId,
                incidentTypeId,
                source,
                occurredAt,
                detectedAt,
                floorAreaId,
                location,
                ongoingActivity,
                description,
                level,
                lowerLevelReason,
                initialResponse,
                createDespiteExisting,
                false,
                null);
    }
}
