package com.eldercare.backend.admission.internal.change.domain;

import java.time.Instant;

/**
 * Ý kiến của chính người cao tuổi về một yêu cầu thay đổi lưu trú (FR-043h, Q-416): mục riêng với xác nhận người đại diện,
 * có nguồn; chỉ thêm, bản mới nhất là hiện hành. {@code contentDigest} là dấu nội dung thực chất của yêu cầu lúc ghi (R18).
 */
public record ResidentOpinion(
        long id,
        long approvalRequestId,
        long residentId,
        Opinion opinion,
        Source source,
        String note,
        String contentDigest,
        long recordedBy,
        Instant recordedAt) {

    public static final int NOTE_MAX = 1000;

    public enum Opinion {
        AGREE,
        DISAGREE,
        UNABLE_TO_EXPRESS
    }

    /** Người cao tuổi tự nói, hoặc nhân viên ghi nhận khi người cao tuổi không diễn đạt được (BR-M01-14). */
    public enum Source {
        RESIDENT,
        STAFF_OBSERVATION
    }
}
