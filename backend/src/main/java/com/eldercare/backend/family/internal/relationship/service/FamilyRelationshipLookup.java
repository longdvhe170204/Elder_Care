package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipView;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.family.internal.relationship.domain.FamilyMember;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Bản cài {@code QuanHeNguoiThanApi} của 002 (012 T006; contracts mục 1.1, research F-04, F-14). */
@Service
class FamilyRelationshipLookup implements FamilyRelationshipApi {

    private final RelationshipQueries queries;

    FamilyRelationshipLookup(RelationshipQueries queries) {
        this.queries = queries;
    }

    @Override
    public List<FamilyRelationshipView> active(long familyMemberId) {
        return queries.activeOfFamilyMember(familyMemberId).stream()
                .map(r -> new FamilyRelationshipView(r.id(), r.residentId()))
                .toList();
    }

    @Override
    public Optional<String> registeredPhoneNumber(long familyMemberId) {
        return queries.familyMember(familyMemberId).map(FamilyMember::phoneNumber);
    }

    @Override
    public boolean isActiveRepresentative(long familyMemberId, long residentId, Instant at) {
        return queries.representativeAt(familyMemberId, residentId, at);
    }

    @Override
    public boolean belongsToResident(long familyMemberId, long residentId) {
        return queries.active(familyMemberId, residentId).isPresent();
    }

    @Override
    public Optional<FamilyRelationshipType> relationship(long familyMemberId, long residentId) {
        return queries.active(familyMemberId, residentId).map(FamilyRelationship::relationship);
    }

    @Override
    public boolean rightEnabled(long familyMemberId, long residentId, FamilyRight right, Instant at) {
        return queries.rightEnabledAt(familyMemberId, residentId, right, at);
    }

    @Override
    public Optional<String> familyMemberName(long familyMemberId) {
        return queries.familyMember(familyMemberId).map(FamilyMember::fullName);
    }
}
