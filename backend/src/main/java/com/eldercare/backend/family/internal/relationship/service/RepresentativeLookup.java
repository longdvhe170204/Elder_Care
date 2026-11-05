package com.eldercare.backend.family.internal.relationship.service;

import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRelationship;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Bản cài {@code NguoiDaiDienPort} của 004 (012 T006; contracts mục 1.1, research F-04). Ngày hôm nay xét tại lúc gọi;
 * ngày khác xét tại cuối ngày đó.
 */
@Service
class RepresentativeLookup implements RepresentativePort {

    private final RelationshipQueries queries;
    private final BusinessTime time;

    RepresentativeLookup(RelationshipQueries queries, BusinessTime time) {
        this.queries = queries;
        this.time = time;
    }

    @Override
    public boolean isActiveRepresentative(long familyMemberId, long residentId, LocalDate date) {
        Instant endOfDay = time.startOfDay(date.plusDays(1)).minusNanos(1000);
        Instant now = time.now();
        return queries.representativeAt(familyMemberId, residentId, now.isBefore(endOfDay) ? now : endOfDay);
    }

    @Override
    public boolean hasRepresentative(long residentId) {
        return queries.activeOfResident(residentId).stream().anyMatch(FamilyRelationship::representative);
    }
}
