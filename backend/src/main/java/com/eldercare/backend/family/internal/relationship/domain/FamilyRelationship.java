package com.eldercare.backend.family.internal.relationship.domain;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import com.eldercare.backend.common.notification.FamilyRight;
import java.time.Instant;
import java.util.Set;

/** Quan hệ người thân – người cao tuổi ({@code quan_he_nguoi_than}; data-model mục 2): vai trò, sáu quyền đang bật. */
public record FamilyRelationship(
        long id,
        long familyMemberId,
        long residentId,
        FamilyRelationshipType relationship,
        boolean representative,
        boolean primaryContact,
        int contactOrder,
        Set<FamilyRight> enabledRights,
        boolean active,
        Instant startedAt,
        Instant endedAt) {

    public FamilyRelationship {
        enabledRights = Set.copyOf(enabledRights);
    }

    /** Quan hệ Hiệu lực tại thời điểm: đã lập và chưa kết thúc. */
    public boolean activeAt(Instant at) {
        return !startedAt.isAfter(at) && (endedAt == null || endedAt.isAfter(at));
    }
}
