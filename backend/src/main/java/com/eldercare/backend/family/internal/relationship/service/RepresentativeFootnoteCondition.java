package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.account.api.spi.FootnoteCondition;
import java.time.Instant;

/**
 * Chú thích ², ⁷ của Phụ lục 27 "chỉ người đại diện" ({@code DieuKienChuThich}; 002 A-05; 012 T006): người thân là người
 * đại diện Hiệu lực của người cao tuổi của đối tượng tại thời điểm xét. Đọc qua {@link FamilyRelationshipApi} như các
 * module khác. Người thân không có quan hệ với người cao tuổi đó thì để lớp phạm vi của 002 từ chối (404, không lộ tồn
 * tại; 002 FR-042), vì lớp vai trò cùng chú thích chạy trước lớp phạm vi.
 */
public record RepresentativeFootnoteCondition(String footnoteCode, FamilyRelationshipApi relationships)
        implements FootnoteCondition {

    @Override
    public boolean isSatisfied(CurrentUser user, Target target, Instant at) {
        if (target.residentId() == null || user.familyMemberId().isEmpty()) {
            return false;
        }
        long familyMemberId = user.familyMemberId().get();
        if (!relationships.belongsToResident(familyMemberId, target.residentId())) {
            return true;
        }
        return relationships.isActiveRepresentative(familyMemberId, target.residentId(), at);
    }
}
