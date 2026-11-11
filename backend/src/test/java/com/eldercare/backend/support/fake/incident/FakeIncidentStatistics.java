package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.incident.api.dto.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/** Nguồn thống kê cấu hình theo đúng phạm vi; không suy quyền toàn viện cho phạm vi khác. */
public class FakeIncidentStatistics implements IncidentStatisticsApi {
    private final Map<ScopeFilter, List<AlertReportView>> alerts = new HashMap<>();
    private final Map<ScopeFilter, List<IncidentReportView>> incidents = new HashMap<>();
    private final Map<ScopeFilter, List<MonitoringView>> monitoring = new HashMap<>();
    private final Map<ScopeFilter, List<Long>> critical = new HashMap<>();
    private final Map<ScopeFilter, Integer> cancelled = new HashMap<>();
    private final Map<ScopeFilter, List<IsolationZoneView>> zones = new HashMap<>();

    public void alerts(ScopeFilter scope, List<AlertReportView> rows) {
        alerts.put(scope, List.copyOf(rows));
    }

    public void incidents(ScopeFilter scope, List<IncidentReportView> rows) {
        incidents.put(scope, List.copyOf(rows));
    }

    public void monitoring(ScopeFilter scope, List<MonitoringView> rows) {
        monitoring.put(scope, List.copyOf(rows));
    }

    public void critical(ScopeFilter scope, List<Long> ids) {
        critical.put(scope, List.copyOf(ids));
    }

    public void cancelled(ScopeFilter scope, int count) {
        cancelled.put(scope, count);
    }

    public void zones(ScopeFilter scope, List<IsolationZoneView> rows) {
        zones.put(scope, List.copyOf(rows));
    }

    @Override
    public Stream<AlertReportView> alerts(ScopeFilter scope, Instant from, Instant to) {
        return alerts.getOrDefault(scope, List.of()).stream()
                .filter(r -> !r.firstAt().isBefore(from) && r.firstAt().isBefore(to));
    }

    @Override
    public Stream<IncidentReportView> incidents(ScopeFilter scope, Instant from, Instant to) {
        return incidents.getOrDefault(scope, List.of()).stream()
                .filter(r -> !r.createdAt().isBefore(from) && r.createdAt().isBefore(to));
    }

    @Override
    public List<MonitoringView> underMonitoring(ScopeFilter scope) {
        return monitoring.getOrDefault(scope, List.of());
    }

    @Override
    public List<Long> criticalResidentIds(ScopeFilter scope) {
        return critical.getOrDefault(scope, List.of());
    }

    @Override
    public int cancelledCount(ScopeFilter scope, Instant from, Instant to) {
        return cancelled.getOrDefault(scope, 0);
    }

    @Override
    public List<IsolationZoneView> containmentZones(ScopeFilter scope) {
        return zones.getOrDefault(scope, List.of());
    }
}
