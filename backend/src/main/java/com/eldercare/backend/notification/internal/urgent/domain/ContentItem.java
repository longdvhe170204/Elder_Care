package com.eldercare.backend.notification.internal.urgent.domain;

import java.time.Instant;

/** Nội dung cần báo của một thông báo gộp vào lượt nhắc gọi (FR-027). */
public record ContentItem(long notificationId, Instant occurredAt, String content) {}
