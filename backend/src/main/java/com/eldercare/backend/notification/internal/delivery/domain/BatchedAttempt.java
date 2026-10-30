package com.eldercare.backend.notification.internal.delivery.domain;

import java.time.Instant;

/** Lần gửi đang dồn trong khung, kèm tầng/khu vực và thời điểm sự kiện của yêu cầu (tin tổng hợp gửi nhân viên). */
public record BatchedAttempt(
        long attemptId, long notificationId, String address, Long floorAreaId, Instant occurredAt) {}
