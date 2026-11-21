package com.eldercare.backend.resident.internal.assessment.domain;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Quyết định khi gia đình không đồng ý đổi mức chăm sóc theo lần đánh giá lại (001 FR-036b, Q-423): giữ mức cũ kèm thỏa
 * thuận lựa chọn có rủi ro và ngày xem lại, hoặc rời viện. Mỗi lần đánh giá tối đa một quyết định.
 */
public record CareLevelChangeDecision(
        long id,
        long residentId,
        long assessmentId,
        Decision decision,
        Long riskAgreementId,
        LocalDate reviewDate,
        String note,
        long recordedBy,
        Instant recordedAt,
        Instant reviewNotifiedAt) {

    public enum Decision {
        KEEP_CURRENT_LEVEL,
        LEAVE_FACILITY
    }
}
