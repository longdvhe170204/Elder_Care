package com.eldercare.backend.incident.internal.alert.domain;

import java.time.Instant;

/**
 * Cảnh báo hoặc sự cố đang mở mà bàn giao ca xét (FR-037, research S-18). {@code group} là {@code ALERT} hoặc
 * {@code INCIDENT} như 008 đọc; {@code provisional} là dấu "tạm nhận, chờ xác nhận bàn giao" (cột
 * {@code handover_acknowledged}).
 */
public record HandoverItem(
        String group,
        long id,
        Long residentId,
        Long floorAreaId,
        String status,
        String level,
        String description,
        Instant createdAt,
        Long assigneeId,
        boolean provisional,
        int version) {
    public static final String ALERT = "ALERT";
    public static final String INCIDENT = "INCIDENT";
}
