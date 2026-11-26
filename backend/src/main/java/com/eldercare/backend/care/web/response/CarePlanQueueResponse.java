package com.eldercare.backend.care.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Một dòng hàng chờ "Cần lập hoặc sửa kế hoạch" (FR-059, Q-448). {@code draftId} rỗng khi chưa có bản đang xử lý;
 * {@code pendingApproval} là bản đã gửi Bác sĩ duyệt, {@code waitingSince} khi đó là lúc gửi.
 */
@Schema(name = "CarePlanQueueRow")
public record CarePlanQueueResponse(
        long residentId,
        String fullName,
        String profileCode,
        List<CarePlanReviewResponse> reasons,
        Long draftId,
        boolean draftReturned,
        boolean pendingApproval,
        String createdBy,
        Instant waitingSince) {}
