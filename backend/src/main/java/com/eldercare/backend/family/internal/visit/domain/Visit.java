package com.eldercare.backend.family.internal.visit.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Lượt thăm ({@code luot_tham}, nhóm 2; R-15: {@code simple_record} loại {@link #KIND}, ngày thăm ở {@code effective_from};
 * người đi cùng {@code luot_tham_di_cung} là cột JSON {@link #companions}; data-model mục 5; FR-029 → FR-039). Khung
 * chép từ CFG-M10-04 lúc đăng ký (FR-032). Trạng thái ở cột {@code status}; trạng thái kết thúc khóa dòng.
 *
 * @param familyMemberId người đăng ký (người thân có quan hệ Hiệu lực)
 * @param registeredBy tài khoản lập lượt (người thân qua cổng hoặc Hành chính tại quầy)
 * @param cancelledBy người hủy: người đăng ký, Hành chính, hệ thống (FR-036, FR-037)
 * @param cancelCause mã lý do khi hệ thống hủy ({@code RESIDENT_NOT_PRESENT}, {@code ISOLATION_ACTIVE},
 *     {@code RELATIONSHIP_ENDED})
 * @param checkOutRemindedAt đã nhắc Hành chính ghi giờ ra (một lần; FR-034)
 */
public record Visit(
        long familyMemberId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int visitorCount,
        Source source,
        long registeredBy,
        List<Companion> companions,
        Instant checkedInAt,
        Long checkedInBy,
        Instant checkedOutAt,
        Long checkedOutBy,
        Canceller cancelledBy,
        String cancelCause,
        String cancellationReason,
        Long cancelledByAccount,
        Instant checkOutRemindedAt) {

    public static final SharedKind<Visit> KIND = SharedKind.of("M10.VISIT", Visit.class);

    public enum Status {
        APPROVED,
        CHECKED_IN,
        CHECKED_OUT,
        NO_SHOW,
        CANCELLED
    }

    public enum Source {
        PORTAL,
        COUNTER
    }

    public enum Canceller {
        REGISTRANT,
        OFFICE,
        SYSTEM
    }

    /**
     * Người đi cùng ({@code NguoiDiCung}); {@code entered} là người thực tế vào, {@code addedAtCheckIn} người được thêm
     * tên lúc vào (FR-033).
     */
    public record Companion(
            String fullName, String relationship, String phoneNumber, boolean entered, boolean addedAtCheckIn) {}

    public Visit {
        companions = companions == null ? List.of() : List.copyOf(companions);
    }

    public static Visit registered(
            long familyMemberId,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            int visitorCount,
            Source source,
            long registeredBy,
            List<Companion> companions) {
        return new Visit(
                familyMemberId,
                date,
                start,
                end,
                visitorCount,
                source,
                registeredBy,
                companions,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public VisitSlot slot() {
        return new VisitSlot(startTime, endTime);
    }

    public Instant slotStart() {
        return slot().startOn(date);
    }

    public Instant slotEnd() {
        return slot().endOn(date);
    }

    public Visit checkedIn(List<Companion> actual, long by, Instant at) {
        return new Visit(
                familyMemberId,
                date,
                startTime,
                endTime,
                visitorCount,
                source,
                registeredBy,
                actual,
                at,
                by,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public Visit checkedOut(long by, Instant at) {
        return new Visit(
                familyMemberId,
                date,
                startTime,
                endTime,
                visitorCount,
                source,
                registeredBy,
                companions,
                checkedInAt,
                checkedInBy,
                at,
                by,
                null,
                null,
                null,
                null,
                checkOutRemindedAt);
    }

    public Visit cancelled(Canceller by, String cause, String reason, Long account) {
        return new Visit(
                familyMemberId,
                date,
                startTime,
                endTime,
                visitorCount,
                source,
                registeredBy,
                companions,
                null,
                null,
                null,
                null,
                by,
                cause,
                reason,
                account,
                null);
    }

    public Visit reminded(Instant at) {
        return new Visit(
                familyMemberId,
                date,
                startTime,
                endTime,
                visitorCount,
                source,
                registeredBy,
                companions,
                checkedInAt,
                checkedInBy,
                checkedOutAt,
                checkedOutBy,
                cancelledBy,
                cancelCause,
                cancellationReason,
                cancelledByAccount,
                at);
    }
}
