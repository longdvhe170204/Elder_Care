package com.eldercare.backend.incident.internal.infection.domain;

import java.time.Instant;

/** Người được thêm vào danh sách tiếp xúc (thân lệnh {@code Contact} của api-007, FR-060, bảng FR-061). */
public record NewContact(
        ContactType type,
        Long subjectId,
        String fullName,
        String relationship,
        String phoneNumber,
        ContactGroup group,
        String source,
        Instant contactFrom,
        Instant contactTo,
        String reason) {}
