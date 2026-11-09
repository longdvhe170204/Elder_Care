package com.eldercare.backend.nutrition.internal.review.domain;

import com.eldercare.backend.nutrition.api.events.ReviewDietNotice.Source;
import java.time.Instant;

/**
 * Một thông báo xem lại chế độ ăn đã gửi Bác sĩ (FR-058): {@code id} là id mốc kỹ thuật, bản gán tham chiếu nó (FR-059);
 * {@code currentAssignmentId} là bản gán Hiệu lực tại lúc báo (rỗng khi dùng chế độ mặc định).
 */
public record ReviewNotice(
        long id, long residentId, Source source, long sourceId, Instant noticedAt, Long currentAssignmentId) {}
