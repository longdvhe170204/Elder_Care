package com.cambae.backend.m13.internal.directcontact.domain;

import java.time.Instant;

/** Một mục "cần liên hệ trực tiếp" ({@code lien_he_truc_tiep}; FR-021) kèm người cao tuổi, tầng của thông báo gốc. */
public record DirectContact(
        long id,
        long notificationId,
        Long familyMemberId,
        String fullName,
        String relationship,
        String phoneNumber,
        String shortContent,
        String status,
        Instant createdAt,
        Long residentId,
        Long floorAreaId) {}
