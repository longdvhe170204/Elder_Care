package com.eldercare.backend.family.web.response;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.family.internal.relationship.service.FamilyMemberService.RelationshipView;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Quan hệ của người cao tuổi (api-012 {@code Relationship}): vai trò, quyền đang bật và quyền có tác dụng (FR-011: bật
 * mà không có tác dụng thì hiển thị "không có tác dụng").
 */
@Schema(name = "Relationship")
public record RelationshipResponse(
        long id,
        FamilyMemberResponse familyMember,
        String relationship,
        boolean representative,
        boolean primaryContact,
        int contactOrder,
        Map<FamilyRight, RightState> rights,
        @Schema(allowableValues = {"ACTIVE", "ENDED"}) String status) {

    @Schema(name = "FamilyRightState")
    public record RightState(boolean enabled, boolean effective) {}

    public static RelationshipResponse of(RelationshipView view) {
        return of(
                view.relationship(),
                view.familyMember(),
                right -> view.effective().getOrDefault(right, false));
    }

    /** Quan hệ trong hồ sơ người thân: quyền có tác dụng chỉ xét ở danh sách theo người cao tuổi. */
    public static RelationshipResponse of(FamilyRelationship relationship, FamilyMember member) {
        return of(relationship, member, relationship.enabledRights()::contains);
    }

    private static RelationshipResponse of(
            FamilyRelationship r, FamilyMember member, Predicate<FamilyRight> effective) {
        Map<FamilyRight, RightState> rights = new LinkedHashMap<>();
        for (FamilyRight right : FamilyRight.values()) {
            rights.put(right, new RightState(r.enabledRights().contains(right), effective.test(right)));
        }
        return new RelationshipResponse(
                r.id(),
                FamilyMemberResponse.of(member),
                r.relationship().name(),
                r.representative(),
                r.primaryContact(),
                r.contactOrder(),
                rights,
                r.active() ? "ACTIVE" : "ENDED");
    }
}
