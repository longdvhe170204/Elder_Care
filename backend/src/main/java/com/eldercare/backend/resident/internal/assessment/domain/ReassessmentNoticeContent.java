package com.eldercare.backend.resident.internal.assessment.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;

/**
 * Nội dung lần báo cần đánh giá lại ở {@code simple_record} loại {@link #KIND} (001 T084, data-model 001 mục 0.1; research
 * H-11): người cao tuổi ở {@code resident_id}, nguồn ở đối tượng cha ({@code entity_type}, {@code entity_id});
 * {@code created_at} là lúc báo (để so với lần đánh giá Đã xác nhận gần nhất); khóa {@link #uniqueKey} chống báo trùng
 * (BR-M01-02, NFR-04, thay {@code UNIQUE (resident_id, basis, source_type, source_id)}). Dòng không khóa: hạn và dấu đã
 * cảnh báo quá hạn còn đổi.
 */
public record ReassessmentNoticeContent(
        ReassessmentBasis basis, Instant notifiedAt, Instant deadlineFrom, Instant overdueAlertedAt) {

    public static final SharedKind<ReassessmentNoticeContent> KIND =
            SharedKind.of("M01.REASSESSMENT_NOTICE", ReassessmentNoticeContent.class);

    /** Trạng thái của dòng (cột {@code status} bắt buộc); lần báo không có máy trạng thái (Q-307). */
    public static final String STATUS = "NOTIFIED";

    public static String uniqueKey(long residentId, ReassessmentBasis basis, String sourceType, long sourceId) {
        return "M01.RAN:" + residentId + ":" + basis.name() + ":" + sourceType + ":" + sourceId;
    }

    public ReassessmentNoticeContent withDeadline(Instant from) {
        return new ReassessmentNoticeContent(basis, notifiedAt, from, null);
    }

    public ReassessmentNoticeContent alerted(Instant at) {
        return new ReassessmentNoticeContent(basis, notifiedAt, deadlineFrom, at);
    }
}
