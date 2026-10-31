package com.eldercare.backend.notification.internal.delivery.domain;

/** Lần gửi kênh ngoài lỗi tạm cần gửi lại (FR-018): lần mới nhất của kênh. */
public record Retry(long notificationId, String channel, String address, String content) {}
