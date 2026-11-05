package com.eldercare.backend.incident.internal.critical.domain;

/** Kết quả xác nhận lại: giữ nguyện vọng, nguyện vọng mới, không liên lạc được (FR-050c). */
public enum ReconfirmationResult {
    KEPT,
    CHANGED,
    UNREACHABLE
}
