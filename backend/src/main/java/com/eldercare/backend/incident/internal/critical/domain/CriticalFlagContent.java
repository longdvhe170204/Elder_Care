package com.eldercare.backend.incident.internal.critical.domain;

import java.time.Instant;

/**
 * Nội dung dấu nguy kịch trên hồ sơ chung (data-model mục 9, R-15 {@code dau_nguy_kich} → {@code simple_record}).
 * Người gỡ rỗng khi gỡ là Hệ thống ("Tự gỡ theo trạng thái"). Nhận định, cảnh báo không đổi sau khi ghi.
 */
public record CriticalFlagContent(
        CriticalFlagType type,
        String assessment,
        long recordedById,
        Instant recordedAt,
        Long alertId,
        Long confirmedById,
        Instant confirmedAt,
        Long removedById,
        Instant removedAt,
        String removalReason) {

    public CriticalFlagContent withAlert(long alert) {
        return new CriticalFlagContent(type, assessment, recordedById, recordedAt, alert, null, null, null, null, null);
    }

    public CriticalFlagContent confirmed(long doctorId, Instant at) {
        return new CriticalFlagContent(
                type, assessment, recordedById, recordedAt, alertId, doctorId, at, null, null, null);
    }

    public CriticalFlagContent removed(Long by, Instant at, String reason) {
        return new CriticalFlagContent(
                type, assessment, recordedById, recordedAt, alertId, confirmedById, confirmedAt, by, at, reason);
    }
}
