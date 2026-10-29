package com.eldercare.backend.administration.correction.api;

import com.eldercare.backend.administration.approval.api.ApprovalRequestView;

/** Kết quả lệnh đính chính: đã lưu (cách A, B) hoặc chờ duyệt (cách C) (FR-026). */
public sealed interface CorrectionResult {

    record Saved(CorrectionView correction) implements CorrectionResult {}

    /** Cách C: yêu cầu {@code M15.CORRECTION} đã lập và gửi duyệt. */
    record PendingApproval(ApprovalRequestView request) implements CorrectionResult {}
}
