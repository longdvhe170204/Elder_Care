package com.eldercare.backend.health.api.events;

import java.time.Instant;

/** Lịch đo đã đổi để 005 cập nhật căn cứ; không tạo công việc (FR-006). */
public record MeasurementScheduleChanged(long scheduleId, long residentId, String eventKey, Instant occurredAt) {}
