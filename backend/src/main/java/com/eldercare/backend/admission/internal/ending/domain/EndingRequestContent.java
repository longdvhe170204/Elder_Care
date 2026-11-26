package com.eldercare.backend.admission.internal.ending.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.admission.api.dto.EndCase;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Yêu cầu kết thúc lưu trú người đại diện gửi trên app (demo 2026-11-26, E làm, chờ B, chủ dự án duyệt; api-004
 * {@code ResidencyEndingRequest}); {@code simple_record} loại {@link #KIND}, trạng thái ở cột {@code status}: Chờ xử lý →
 * Đã xử lý (Hành chính lập hồ sơ kết thúc từ yêu cầu) / Từ chối (kèm lý do) / Đã hủy (người đại diện hủy).
 */
public record EndingRequestContent(
        long familyMemberId,
        EndCase endCase,
        LocalDate expectedEndDate,
        String reason,
        Instant createdAt,
        String decisionReason,
        Long decidedBy,
        Instant decidedAt,
        Long residencyEndingId) {

    public static final SharedKind<EndingRequestContent> KIND =
            SharedKind.of("M02.ENDING_REQUEST", EndingRequestContent.class);

    public static final String PENDING = "PENDING";
    public static final String HANDLED = "HANDLED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final java.util.Set<String> ALL = java.util.Set.of(PENDING, HANDLED, REJECTED, CANCELLED);

    public static EndingRequestContent submitted(
            long familyMemberId, EndCase endCase, LocalDate expectedEndDate, String reason, Instant at) {
        return new EndingRequestContent(familyMemberId, endCase, expectedEndDate, reason, at, null, null, null, null);
    }

    public EndingRequestContent decided(Long by, Instant at, String decisionReason, Long endingId) {
        return new EndingRequestContent(
                familyMemberId, endCase, expectedEndDate, reason, createdAt, decisionReason, by, at, endingId);
    }
}
