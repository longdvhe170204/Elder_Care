package com.eldercare.backend.family.internal.right.service;

import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.api.FamilyRightsApi;
import com.eldercare.backend.family.api.dto.FamilyMemberView;
import com.eldercare.backend.family.api.dto.IdDocumentView;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.family.internal.relationship.service.RelationshipQueries;
import com.eldercare.backend.family.internal.right.domain.EffectiveRights;
import com.eldercare.backend.resident.api.ConsentApi;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Bản cài {@code QuyenNguoiThanApi} (012 T003, T006; contracts mục 1.2). Bản đồng ý xét theo bản Hiệu lực lúc gọi: 001
 * chưa có tra cứu theo thời điểm, và mọi lần gọi là lúc người thân xem.
 */
@Service
class FamilyRightsLookup implements FamilyRightsApi {

    private final RelationshipQueries queries;
    private final ConsentApi consents;

    FamilyRightsLookup(RelationshipQueries queries, ConsentApi consents) {
        this.queries = queries;
        this.consents = consents;
    }

    @Override
    public boolean hasActiveRelationship(long familyMemberId, long residentId) {
        return queries.active(familyMemberId, residentId).isPresent();
    }

    @Override
    public boolean rightEffective(long familyMemberId, long residentId, FamilyRight right, Instant at) {
        boolean enabled = queries.rightEnabledAt(familyMemberId, residentId, right, at);
        // chỉ xem sức khỏe cần bản đồng ý; tránh gọi 001 khi không cần
        boolean consentIncludes =
                enabled && right == FamilyRight.VIEW_HEALTH && consents.includes(residentId, familyMemberId);
        return EffectiveRights.effective(right, enabled, consentIncludes);
    }

    @Override
    public boolean isRepresentative(long familyMemberId, long residentId, Instant at) {
        return queries.representativeAt(familyMemberId, residentId, at);
    }

    @Override
    public boolean canViewBelongings(long familyMemberId, long residentId, Instant at) {
        return queries.representativeAt(familyMemberId, residentId, at)
                || queries.rightEnabledAt(familyMemberId, residentId, FamilyRight.PICKUP_ALLOWED, at);
    }

    @Override
    public List<FamilyMemberView> representatives(long residentId) {
        return views(queries.activeOfResident(residentId).stream()
                .filter(FamilyRelationship::representative)
                .toList());
    }

    @Override
    public Optional<FamilyMemberView> primaryContact(long residentId) {
        return views(queries.activeOfResident(residentId).stream()
                        .filter(FamilyRelationship::primaryContact)
                        .toList())
                .stream()
                .findFirst();
    }

    @Override
    public List<FamilyMemberView> allowedPickersAt(long residentId, Instant at) {
        return views(queries.withRightAt(residentId, FamilyRight.PICKUP_ALLOWED, at));
    }

    @Override
    public Optional<IdDocumentView> idDocument(long familyMemberId) {
        return queries.familyMember(familyMemberId)
                .filter(m -> m.idDocumentNumber() != null)
                .map(m -> new IdDocumentView(m.idDocumentType(), m.idDocumentNumber()));
    }

    private List<FamilyMemberView> views(List<FamilyRelationship> relationships) {
        return relationships.stream()
                .flatMap(r -> queries.familyMember(r.familyMemberId()).stream()
                        .map(m -> new FamilyMemberView(m.id(), m.fullName(), m.phoneNumber(), r.relationship())))
                .toList();
    }
}
