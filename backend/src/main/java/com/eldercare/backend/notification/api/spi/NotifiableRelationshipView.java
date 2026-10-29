package com.eldercare.backend.notification.api.spi;

import com.eldercare.backend.common.notification.FamilyRight;
import java.util.Optional;
import java.util.Set;

/** Một quan hệ Hiệu lực của người thân ({@code QuanHeNhanThongBaoView}). */
public record NotifiableRelationshipView(
        long familyMemberId,
        boolean primaryContact,
        boolean representative,
        Set<FamilyRight> rights,
        Optional<Long> portalAccountId,
        boolean portalAccountActive,
        Optional<String> phoneNumber) {}
