package com.cambae.backend.m15.correction.api;

/** Kết quả lệnh đính chính: đã lưu (cách A, B) hoặc chờ duyệt (cách C) (FR-026). */
public sealed interface CorrectionResult {

    record Saved(CorrectionView correction) implements CorrectionResult {}

    record PendingApproval(long approvalRequestId) implements CorrectionResult {}
}
