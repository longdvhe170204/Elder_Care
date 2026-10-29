package com.eldercare.backend.notification.web.response;

import com.eldercare.backend.notification.internal.directcontact.domain.DirectContact;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** Một mục "cần liên hệ trực tiếp" (schema {@code DirectContact}; FR-021). */
public record DirectContactBody(
        long id,
        String fullName,
        String relationship,
        String phoneNumber,
        String shortContent,
        Status status,
        OffsetDateTime createdAt) {

    public enum Status {
        PENDING,
        CONTACTED,
        UNREACHABLE
    }

    public static DirectContactBody of(DirectContact r) {
        return new DirectContactBody(
                r.id(),
                r.fullName(),
                r.relationship(),
                r.phoneNumber(),
                r.shortContent(),
                Status.valueOf(r.status()),
                r.createdAt().atOffset(ZoneOffset.UTC));
    }
}
