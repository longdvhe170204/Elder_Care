package com.eldercare.backend.incident.internal.alert.domain;

/**
 * Một lần bấm SOS ({@code lan_bam_sos}, data-model mục 7; R-15: {@code record_event} kind {@code M05.SOS_PRESS}, cha là
 * người cao tuổi, khóa duy nhất theo thiết bị và {@code Idempotency-Key}). Cảnh báo được tạo hoặc gộp nối qua dòng nguồn
 * {@code M05.ALERT_SOURCE}. Lần bấm bị từ chối không ghi.
 */
public record SosPressContent(
        long residentId,
        long accountId,
        String deviceId,
        String idempotencyKey,
        Long roomId,
        Long bedId,
        String residentStatus) {}
