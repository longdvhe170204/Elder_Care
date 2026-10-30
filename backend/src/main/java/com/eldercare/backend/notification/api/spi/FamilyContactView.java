package com.eldercare.backend.notification.api.spi;

import java.util.Optional;

/** Liên hệ hiện hành của một người thân ({@code LienHeNguoiThanView}). */
public record FamilyContactView(
        long familyMemberId,
        String fullName,
        Optional<String> phoneNumber,
        Optional<Long> portalAccountId,
        boolean portalAccountActive) {}
