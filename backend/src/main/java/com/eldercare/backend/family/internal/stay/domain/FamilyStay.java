package com.eldercare.backend.family.internal.stay.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.util.Map;

/**
 * Lượt người thân ở lại ({@code luot_o_lai}, nhóm 2; R-15: {@code simple_record} loại {@link #KIND}, trạng thái ở cột
 * {@code status}; data-model mục 6; FR-040 → FR-045). Lịch sử gia hạn là sự kiện {@link Extension}; đêm đã phát là dấu
 * {@code reminder_marker}.
 *
 * @param location {@code PHONG_NCT} (phòng của người cao tuổi) hoặc mã vị trí khác do cơ sở khai báo
 * @param roommateOpinion ý kiến người cùng phòng (hoặc người đại diện của họ) và kết quả, ghi lúc xác nhận khi phòng có
 *     từ hai giường đang dùng
 */
public record FamilyStay(
        long familyMemberId,
        String location,
        String reason,
        boolean mealRegistered,
        Instant expectedStartAt,
        Instant expectedEndAt,
        Instant actualStartAt,
        Instant actualEndAt,
        Long confirmedBy,
        Instant confirmedAt,
        Map<String, Object> roommateOpinion,
        String rejectionReason,
        String cancellationReason) {

    public static final SharedKind<FamilyStay> KIND = SharedKind.of("M10.FAMILY_STAY", FamilyStay.class);

    public enum Status {
        PENDING_CONFIRMATION,
        CONFIRMED,
        REJECTED,
        STAYING,
        ENDED,
        CANCELLED
    }

    /** Một lần gia hạn ({@code luot_o_lai_gia_han}, nhóm 3; R-15: {@code record_event} loại {@link #KIND}). */
    public record Extension(Instant previousExpectedEndAt, Instant newExpectedEndAt, String reason) {
        public static final SharedKind<Extension> KIND = SharedKind.of("M10.FAMILY_STAY_EXTENSION", Extension.class);
    }

    public static FamilyStay registered(
            long familyMemberId,
            String location,
            String reason,
            boolean mealRegistered,
            Instant expectedStartAt,
            Instant expectedEndAt) {
        return new FamilyStay(
                familyMemberId,
                location,
                reason,
                mealRegistered,
                expectedStartAt,
                expectedEndAt,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /** Khoảng dùng để xét chồng thời gian: thực tế khi đã có, còn lại là dự kiến. */
    public Instant from() {
        return actualStartAt != null ? actualStartAt : expectedStartAt;
    }

    public Instant to() {
        return actualEndAt != null ? actualEndAt : expectedEndAt;
    }

    public boolean overlaps(Instant start, Instant end) {
        return from().isBefore(end) && start.isBefore(to());
    }

    public FamilyStay confirmed(long by, Instant at, Map<String, Object> opinion) {
        return new FamilyStay(
                familyMemberId,
                location,
                reason,
                mealRegistered,
                expectedStartAt,
                expectedEndAt,
                actualStartAt,
                actualEndAt,
                by,
                at,
                opinion,
                rejectionReason,
                cancellationReason);
    }

    public FamilyStay rejected(long by, Instant at, String why) {
        return new FamilyStay(
                familyMemberId,
                location,
                reason,
                mealRegistered,
                expectedStartAt,
                expectedEndAt,
                actualStartAt,
                actualEndAt,
                by,
                at,
                roommateOpinion,
                why,
                cancellationReason);
    }

    public FamilyStay cancelled(String why) {
        return new FamilyStay(
                familyMemberId,
                location,
                reason,
                mealRegistered,
                expectedStartAt,
                expectedEndAt,
                actualStartAt,
                actualEndAt,
                confirmedBy,
                confirmedAt,
                roommateOpinion,
                rejectionReason,
                why);
    }

    public FamilyStay started(Instant at) {
        return new FamilyStay(
                familyMemberId,
                location,
                reason,
                mealRegistered,
                expectedStartAt,
                expectedEndAt,
                at,
                null,
                confirmedBy,
                confirmedAt,
                roommateOpinion,
                rejectionReason,
                cancellationReason);
    }

    public FamilyStay extended(Instant newExpectedEndAt) {
        return new FamilyStay(
                familyMemberId,
                location,
                reason,
                mealRegistered,
                expectedStartAt,
                newExpectedEndAt,
                actualStartAt,
                actualEndAt,
                confirmedBy,
                confirmedAt,
                roommateOpinion,
                rejectionReason,
                cancellationReason);
    }

    public FamilyStay ended(Instant at) {
        return new FamilyStay(
                familyMemberId,
                location,
                reason,
                mealRegistered,
                expectedStartAt,
                expectedEndAt,
                actualStartAt,
                at,
                confirmedBy,
                confirmedAt,
                roommateOpinion,
                rejectionReason,
                cancellationReason);
    }
}
