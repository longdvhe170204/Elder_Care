package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.api.events.ReviewDietNotice.Source;
import com.eldercare.backend.nutrition.internal.review.domain.ReviewNotice;
import java.time.Instant;

/**
 * Thông báo xem lại chế độ ăn đã gửi Bác sĩ (FR-058, FR-059): {@code source} là {@code PROLONGED_POOR_EATING} hoặc
 * {@code WEIGHT_LOSS}; {@code currentAssignmentId} là bản gán Hiệu lực tại lúc báo (vắng khi dùng chế độ mặc định).
 */
public record ReviewNoticeResponse(
        long id, long residentId, Source source, long sourceId, Instant noticedAt, Long currentAssignmentId) {

    public static ReviewNoticeResponse of(ReviewNotice n) {
        return new ReviewNoticeResponse(
                n.id(), n.residentId(), n.source(), n.sourceId(), n.noticedAt(), n.currentAssignmentId());
    }
}
