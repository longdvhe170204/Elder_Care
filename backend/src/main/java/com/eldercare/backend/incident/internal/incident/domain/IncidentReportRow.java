package com.eldercare.backend.incident.internal.incident.domain;

import com.eldercare.backend.incident.api.dto.IncidentReportView;
import java.time.Instant;

/** Một dòng sự cố chưa hủy cho thống kê của 016, kèm loại hiện hành để lấy dấu "không thuộc sức khỏe" (FR-081, T061). */
public record IncidentReportRow(
        long id,
        Long residentId,
        String level,
        String levelAtReceived,
        long currentTypeId,
        String typeCode,
        Long floorAreaId,
        Instant createdAt,
        Instant receivedAt,
        int escalationLevel,
        String status) {
    public IncidentReportView view(boolean nonHealth) {
        return new IncidentReportView(
                id,
                residentId,
                level,
                levelAtReceived,
                typeCode,
                nonHealth,
                floorAreaId,
                createdAt,
                receivedAt,
                escalationLevel,
                status);
    }
}
