package com.eldercare.backend.incident.internal.infection.service;

import com.eldercare.backend.care.api.spi.ActivityHealthGatePort;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bản thật {@link ActivityHealthGatePort} cho 014 (FR-015 (c), (g), FR-018; chủ dự án duyệt 2026-11-05): chỉ đọc vùng
 * khoanh hiện hành, dấu nghi nhiễm, cảnh báo hoặc sự cố mức Trung bình trở lên đang mở.
 */
@Component
@Transactional(readOnly = true)
public class IncidentActivityHealthGate implements ActivityHealthGatePort {
    private final DefaultIsolationApi isolation;
    private final IncidentStore incidents;
    private final AlertStore alerts;

    public IncidentActivityHealthGate(DefaultIsolationApi isolation, IncidentStore incidents, AlertStore alerts) {
        this.isolation = isolation;
        this.incidents = incidents;
        this.alerts = alerts;
    }

    @Override
    public boolean residentIsolated(long residentId, Instant at) {
        return !isolation.activeByResident(residentId, at).isEmpty();
    }

    @Override
    public boolean locationIsolated(Long roomId, Long areaId, Long floorAreaId, Instant at) {
        if (roomId != null && !isolation.activeByRoom(roomId, at).isEmpty()) return true;
        if (floorAreaId != null && !isolation.activeByFloorArea(floorAreaId, at).isEmpty()) return true;
        return areaId != null
                && isolation.activeZones().stream()
                        .anyMatch(zone -> zone.areaIds().contains(areaId)
                                && !zone.startsAt().isAfter(at)
                                && (zone.endsAt() == null || at.isBefore(zone.endsAt())));
    }

    @Override
    public boolean suspectedInfection(long residentId, Instant at) {
        return incidents.suspectedAt(residentId, at);
    }

    @Override
    public boolean hasOpenMediumOrHigher(long residentId) {
        return alerts.hasOpenAtLeastMedium(residentId) || incidents.hasOpenAtLeastMedium(residentId);
    }
}
