package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import com.eldercare.backend.family.internal.visit.service.VisitQueries;
import com.eldercare.backend.incident.api.dto.EmergencyFamilyContactView;
import com.eldercare.backend.incident.api.dto.VisitTraceView;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Bản cài {@code NguoiThanKhanCapPort} của 007 (012 T006; contracts mục 1.1). Lượt thăm cho truy vết (FR-038) đọc qua
 * {@link VisitQueries} (US4, T024).
 */
@Service
class EmergencyFamilyLookup implements EmergencyFamilyPort {

    private final RelationshipQueries queries;
    private final VisitQueries visits;

    EmergencyFamilyLookup(RelationshipQueries queries, VisitQueries visits) {
        this.queries = queries;
        this.visits = visits;
    }

    @Override
    public Optional<EmergencyFamilyContactView> primaryContact(long residentId) {
        return queries.activeOfResident(residentId).stream()
                .filter(FamilyRelationship::primaryContact)
                .findFirst()
                .flatMap(this::contact);
    }

    @Override
    public List<EmergencyFamilyContactView> representatives(long residentId) {
        return queries.activeOfResident(residentId).stream()
                .filter(FamilyRelationship::representative)
                .flatMap(r -> contact(r).stream())
                .toList();
    }

    @Override
    public List<VisitTraceView> visitsInRange(long residentId, Instant from, Instant to) {
        return visits.inRange(residentId, from, to);
    }

    private Optional<EmergencyFamilyContactView> contact(FamilyRelationship r) {
        return queries.familyMember(r.familyMemberId())
                .map(m -> new EmergencyFamilyContactView(m.id(), m.fullName(), Optional.of(m.phoneNumber())));
    }
}
