package com.eldercare.backend.incident.internal.alert.domain;

import java.time.Instant;

/**
 * Lệnh yêu cầu tạo hoặc gộp cảnh báo từ các nguồn nghiệp vụ (FR-027, FR-028, S-08). {@code requesterId} là tài khoản
 * người bấm nút hỗ trợ khẩn cấp (FR-047i): cảnh báo chưa gắn người cao tuổi gộp theo người bấm. {@code sourceStaffId} là
 * nhân viên thực hiện công việc nguồn (FR-035, mức Trung bình) khi nguồn cho biết; không có thì để rỗng.
 */
public record CreateAlertCommand(
        Long residentId,
        Long floorAreaId,
        String type,
        String typeLock,
        IncidentLevel level,
        Instant occurredAt,
        String sourceType,
        Long sourceId,
        String description,
        Long createdById,
        Long requesterId,
        Long sourceStaffId) {

    public CreateAlertCommand(
            Long residentId,
            Long floorAreaId,
            String type,
            String typeLock,
            IncidentLevel level,
            Instant occurredAt,
            String sourceType,
            Long sourceId,
            String description,
            Long createdById,
            Long requesterId) {
        this(
                residentId,
                floorAreaId,
                type,
                typeLock,
                level,
                occurredAt,
                sourceType,
                sourceId,
                description,
                createdById,
                requesterId,
                null);
    }

    public CreateAlertCommand(
            Long residentId,
            Long floorAreaId,
            String type,
            String typeLock,
            IncidentLevel level,
            Instant occurredAt,
            String sourceType,
            Long sourceId,
            String description,
            Long createdById) {
        this(
                residentId,
                floorAreaId,
                type,
                typeLock,
                level,
                occurredAt,
                sourceType,
                sourceId,
                description,
                createdById,
                null);
    }
}
