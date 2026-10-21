package com.cambae.backend.m13.internal.domain;

/** Loại sự kiện của thông báo ({@code su_kien_thong_bao.loai}; research B-09). */
public enum NotificationEventType {
    HOLD,
    RESCHEDULE,
    SEND,
    NOT_SENT,
    VIEW,
    ACKNOWLEDGE,
    UNREACHABLE,
    HIDE
}
