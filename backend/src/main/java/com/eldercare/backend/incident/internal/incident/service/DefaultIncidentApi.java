package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.incident.api.IncidentApi;
import com.eldercare.backend.incident.api.dto.IncidentView;
import com.eldercare.backend.incident.api.dto.MonitoringFlagView;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.critical.persistence.CriticalCareStore;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContactPersonStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản thật {@link IncidentApi} (module-interfaces 007 mục 1.7; 013 FR-021, FR-025): trạng thái theo enum
 * {@code IncidentStatus} của api-007, Đã hủy là {@code VOIDED}; khoảng nửa mở [from, to) theo thời điểm lập.
 */
@Service
@Transactional(readOnly = true)
public class DefaultIncidentApi implements IncidentApi {
    private static final String STORED_CANCELLED = "CANCELLED";
    private static final String VOIDED = "VOIDED";

    private final IncidentStore incidents;
    private final AlertStore alerts;
    private final ContactPersonStore contacts;
    private final FallMonitoringStore fallMonitoring;
    private final CriticalCareStore critical;

    public DefaultIncidentApi(
            IncidentStore incidents,
            AlertStore alerts,
            ContactPersonStore contacts,
            FallMonitoringStore fallMonitoring,
            CriticalCareStore critical) {
        this.incidents = incidents;
        this.alerts = alerts;
        this.contacts = contacts;
        this.fallMonitoring = fallMonitoring;
        this.critical = critical;
    }

    @Override
    public Optional<IncidentView> view(long incidentId) {
        return incidents.sourceView(incidentId).map(DefaultIncidentApi::published);
    }

    @Override
    public List<IncidentView> byResident(long residentId, Instant from, Instant to, boolean excludeVoided) {
        return incidents.sourceByResident(residentId, from, to, excludeVoided).stream()
                .map(DefaultIncidentApi::published)
                .toList();
    }

    @Override
    public boolean hasOpenItem(long residentId) {
        return alerts.hasOpenAlert(residentId) || incidents.hasOpen(residentId);
    }

    @Override
    public MonitoringFlagView monitoringFlag(long residentId, Instant at) {
        boolean postFall = fallMonitoring
                .active(residentId)
                .map(row -> !at.isBefore(row.content().anchor())
                        && at.isBefore(row.content().endsAt()))
                .orElse(false);
        return new MonitoringFlagView(
                incidents.suspectedAt(residentId, at),
                contacts.listedAt(residentId, at),
                postFall,
                critical.openFlag(residentId).isPresent());
    }

    private static IncidentView published(IncidentView row) {
        return STORED_CANCELLED.equals(row.status())
                ? new IncidentView(
                        row.id(), row.residentId(), row.type(), row.level(), VOIDED, row.createdAt(), row.floorAreaId())
                : row;
    }
}
