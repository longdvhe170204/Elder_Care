package com.eldercare.backend.health.internal.schedule.domain;

import java.time.Instant;

/**
 * Lịch đo đã lưu (data-model {@code lich_do}, R-15 → {@code monitoring_setting} kind MEASUREMENT_SCHEDULE): trạng thái
 * {@code PENDING}, {@code EFFECTIVE}, {@code STOPPED}, {@code EXPIRED}; {@code replaces} là lịch cũ khi Thay đổi lịch đo.
 */
public record MeasurementSchedule(
        long id,
        long residentId,
        NewMeasurementSchedule content,
        String status,
        Long replaces,
        Instant stoppedAt,
        String stopReason,
        int version) {

    /** Là căn cứ lập công việc tại {@code at}: đã bắt đầu, chưa tới kết thúc, chưa tới thời điểm ngừng. */
    public boolean basisAt(Instant at) {
        return !content.startAt().isAfter(at)
                && (content.endAt() == null || at.isBefore(content.endAt()))
                && (stoppedAt == null || at.isBefore(stoppedAt));
    }
}
