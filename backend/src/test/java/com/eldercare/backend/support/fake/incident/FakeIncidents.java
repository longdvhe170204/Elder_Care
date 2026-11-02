package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.incident.api.AlertApi;
import com.eldercare.backend.incident.api.IncidentApi;
import com.eldercare.backend.incident.api.dto.IncidentView;
import com.eldercare.backend.incident.api.dto.MonitoringFlagView;
import java.time.Instant;
import java.util.*;

/** Nguồn sự cố do test đặt; không thực thi nghiệp vụ ghi hoặc tự sinh dữ liệu. */
public class FakeIncidents implements IncidentApi {
    private final AlertApi alerts;
    private final Map<Long, IncidentView> rows = new LinkedHashMap<>();
    private final Map<Long, MonitoringFlagView> flags = new HashMap<>();

    public FakeIncidents(AlertApi alerts) {
        this.alerts = alerts;
    }

    public void put(IncidentView row) {
        rows.put(row.id(), row);
    }

    public void flag(long residentId, MonitoringFlagView flag) {
        flags.put(residentId, flag);
    }

    @Override
    public Optional<IncidentView> view(long id) {
        return Optional.ofNullable(rows.get(id));
    }

    @Override
    public List<IncidentView> byResident(long residentId, Instant from, Instant to, boolean excludeVoided) {
        return rows.values().stream()
                .filter(r -> Objects.equals(r.residentId(), residentId))
                .filter(r -> !excludeVoided || !"VOIDED".equals(r.status()))
                .filter(r -> !r.createdAt().isBefore(from) && r.createdAt().isBefore(to))
                .toList();
    }

    @Override
    public boolean hasOpenItem(long residentId) {
        return alerts.hasOpenAlert(residentId)
                || rows.values().stream()
                        .anyMatch(r -> Objects.equals(r.residentId(), residentId)
                                && Set.of("NEW", "IN_PROGRESS", "MONITORING").contains(r.status()));
    }

    @Override
    public MonitoringFlagView monitoringFlag(long residentId, Instant at) {
        return flags.getOrDefault(residentId, new MonitoringFlagView(false, false, false, false));
    }
}
