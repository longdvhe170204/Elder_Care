package com.eldercare.backend.care.api.dto;

import java.time.Instant;

/** Nguồn bàn giao 005: trạng thái hiện hành; chỉ đọc, không tự lập mục. */
public record TaskHandoverView(
        long id,
        Long residentId,
        Long roomId,
        String description,
        Instant plannedAt,
        String status,
        String level,
        String locationType,
        Long locationId,
        Long floorAreaId) {
    /** Giữ caller cũ cho công việc gắn người cao tuổi. */
    public TaskHandoverView(
            long id, long residentId, Long roomId, String description, Instant plannedAt, String status, String level) {
        this(id, residentId, roomId, description, plannedAt, status, level, null, null, null);
    }
}
