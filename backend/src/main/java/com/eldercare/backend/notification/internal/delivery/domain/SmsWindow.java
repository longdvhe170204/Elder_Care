package com.eldercare.backend.notification.internal.delivery.domain;

import java.time.Instant;

/** Khung gộp tin nhắn đang mở của một người nhận (FR-016b). */
public record SmsWindow(long id, String recipientKey, Instant endsAt) {}
