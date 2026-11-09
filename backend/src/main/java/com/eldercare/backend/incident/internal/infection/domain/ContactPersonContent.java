package com.eldercare.backend.incident.internal.infection.domain;

import java.time.Instant;

/**
 * Một người trong danh sách tiếp xúc (data-model mục 10 {@code nguoi_tiep_xuc}): nhóm, nguồn, khoảng tiếp xúc; khoảng
 * theo dõi chỉ có với người cao tuổi, đặt khi danh sách được xác nhận hoặc khi bổ sung (FR-062).
 */
public record ContactPersonContent(
        ContactType type,
        Long subjectId,
        String fullName,
        String relationship,
        String phoneNumber,
        ContactGroup group,
        String source,
        Instant contactFrom,
        Instant contactTo,
        String addReason,
        Instant monitoredFrom,
        Instant monitoredTo,
        Instant excludedAt,
        String exclusionReason,
        boolean hasNotificationChannel) {

    public ContactPersonContent monitored(Instant from, Instant to) {
        return new ContactPersonContent(
                type,
                subjectId,
                fullName,
                relationship,
                phoneNumber,
                group,
                source,
                contactFrom,
                contactTo,
                addReason,
                type == ContactType.RESIDENT ? from : null,
                type == ContactType.RESIDENT ? to : null,
                null,
                null,
                hasNotificationChannel);
    }

    public ContactPersonContent excluded(Instant at, String reason) {
        return new ContactPersonContent(
                type,
                subjectId,
                fullName,
                relationship,
                phoneNumber,
                group,
                source,
                contactFrom,
                contactTo,
                addReason,
                monitoredFrom,
                monitoredTo,
                at,
                reason,
                hasNotificationChannel);
    }

    /** Dấu "tiếp xúc" tại {@code at}: trong khoảng theo dõi và chưa bị loại. */
    public boolean monitoredAt(Instant at) {
        return monitoredFrom != null
                && !at.isBefore(monitoredFrom)
                && at.isBefore(monitoredTo)
                && (excludedAt == null || at.isBefore(excludedAt));
    }
}
