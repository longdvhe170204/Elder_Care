package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.health.api.dto.VitalSignReadingView;
import com.eldercare.backend.incident.api.FamilyPortalHealthApi;
import com.eldercare.backend.incident.api.dto.FamilyIncidentView;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeView;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.emergency.persistence.EmergencyStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.resident.api.ConsentApi;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nguồn sức khỏe cho cổng người thân (module-interfaces mục 1.9; T061a). 012 kiểm quan hệ và quyền; 007 chỉ trả phần
 * sức khỏe, chỉ số khi bản đồng ý của người cao tuổi bao gồm người thân đó (BR-M01-08). Bỏ sự cố Đã hủy (FR-046a).
 */
@Service
public class DefaultFamilyPortalHealth implements FamilyPortalHealthApi {
    private static final Set<String> NOT_CANCELLED = Set.of("NEW", "IN_PROGRESS", "MONITORING", "CLOSED");

    private final IncidentStore incidents;
    private final IncidentProgressionStore progressions;
    private final EmergencyStore emergencies;
    private final IncidentCatalogService catalog;
    private final ConsentApi consents;
    private final VitalSignApi vitalSigns;

    public DefaultFamilyPortalHealth(
            IncidentStore incidents,
            IncidentProgressionStore progressions,
            EmergencyStore emergencies,
            IncidentCatalogService catalog,
            ConsentApi consents,
            VitalSignApi vitalSigns) {
        this.incidents = incidents;
        this.progressions = progressions;
        this.emergencies = emergencies;
        this.catalog = catalog;
        this.consents = consents;
        this.vitalSigns = vitalSigns;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyIncidentView> incidents(long residentId, long familyMemberId, Instant from, Instant to) {
        boolean health = consents.includes(residentId, familyMemberId);
        Map<Long, IncidentTypeView> types =
                catalog.incidentTypes().stream().collect(Collectors.toMap(IncidentTypeView::id, Function.identity()));
        return incidents.search(NOT_CANCELLED, residentId, null, true, Set.of(), Set.of()).stream()
                .filter(incident -> {
                    var at = incident.occurredAt() != null ? incident.occurredAt() : incident.discoveredAt();
                    return !at.isBefore(from) && at.isBefore(to);
                })
                .map(incident -> FamilyIncidentPartitioner.partition(
                        incident,
                        types.get(incident.currentTypeId()),
                        health
                                ? progressions.list(incident.id()).stream()
                                        .map(item -> (String) item.get("content"))
                                        .toList()
                                : List.of(),
                        health
                                ? emergencies.latestSummary(incident.id()).map(s -> s.receivingFacility())
                                : Optional.empty(),
                        health))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VitalSignReadingView> vitalSigns(long residentId, long familyMemberId, Instant from, Instant to) {
        if (!consents.includes(residentId, familyMemberId)) return List.of();
        return vitalSigns.vitalSignIndicatorIds().stream()
                .flatMap(indicator -> vitalSigns.withinRange(residentId, indicator, from, to).stream())
                .sorted(Comparator.comparing(VitalSignReadingView::measuredAt))
                .toList();
    }
}
