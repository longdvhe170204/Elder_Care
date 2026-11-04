package com.eldercare.backend.incident.internal.infection.service;

import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContactFollowUpStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.roombedallocation.api.spi.CordonedArea;
import java.time.Instant;
import java.util.HashSet;
import org.springframework.context.annotation.Fallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nguồn thật 007 cho điều kiện phân bổ và khử khuẩn giường của 003. */
@Service
@Fallback
@Transactional(readOnly = true)
public class IncidentAllocationStatus implements AllocationInfectionStatus {
    private final ContainmentStore containments;
    private final ContactFollowUpStore contacts;
    private final IncidentStore incidents;

    public IncidentAllocationStatus(
            ContainmentStore containments, ContactFollowUpStore contacts, IncidentStore incidents) {
        this.containments = containments;
        this.contacts = contacts;
        this.incidents = incidents;
    }

    @Override
    public CordonedArea cordonedAt(Instant at) {
        var floors = new HashSet<Long>();
        var areas = new HashSet<Long>();
        for (var scope : containments.scopesAt(at)) {
            floors.addAll(scope.floorIds());
            areas.addAll(scope.areaIds());
        }
        return new CordonedArea(floors, areas);
    }

    @Override
    public boolean listedAt(long residentId, Instant at) {
        return incidents.suspectedAt(residentId, at) || contacts.listedAt(residentId, at);
    }
}
