package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.incident.api.IsolationApi;
import com.eldercare.backend.incident.api.dto.AlertReportView;
import com.eldercare.backend.incident.api.dto.IncidentReportView;
import com.eldercare.backend.incident.api.dto.IsolationZoneView;
import com.eldercare.backend.incident.api.dto.MonitoringView;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeView;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.critical.persistence.CriticalCareStore;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentReportStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContactPersonStore;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nguồn thống kê cảnh báo, sự cố cho 016 (FR-081; module-interfaces mục 1.10; T061): đọc, không tính chỉ tiêu. Phạm vi:
 * toàn viện, hoặc người cao tuổi trong tập, hoặc tầng/phòng của người đó tại thời điểm của dòng (cảnh báo, sự cố không
 * có người cao tuổi xét theo tầng ghi trên dòng); phạm vi rỗng không trả dữ liệu.
 */
@Service
public class DefaultIncidentStatistics implements IncidentStatisticsApi {
    static final String POST_FALL = "POST_FALL";
    static final String CONTACT = "CONTACT";

    private final IncidentReportStore reports;
    private final FallMonitoringStore falls;
    private final ContactPersonStore contacts;
    private final CriticalCareStore critical;
    private final IncidentCatalogService catalog;
    private final ResidentLocationApi locations;
    private final IsolationApi isolation;

    public DefaultIncidentStatistics(
            IncidentReportStore reports,
            FallMonitoringStore falls,
            ContactPersonStore contacts,
            CriticalCareStore critical,
            IncidentCatalogService catalog,
            ResidentLocationApi locations,
            IsolationApi isolation) {
        this.reports = reports;
        this.falls = falls;
        this.contacts = contacts;
        this.critical = critical;
        this.catalog = catalog;
        this.locations = locations;
        this.isolation = isolation;
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<AlertReportView> alerts(ScopeFilter scope, Instant from, Instant to) {
        return reports.alerts(from, to).stream()
                .filter(row -> inScope(scope, row.residentId(), row.floorAreaId(), row.firstAt()))
                .toList()
                .stream();
    }

    @Override
    @Transactional(readOnly = true)
    public Stream<IncidentReportView> incidents(ScopeFilter scope, Instant from, Instant to) {
        Map<Long, IncidentTypeView> types =
                catalog.incidentTypes().stream().collect(Collectors.toMap(IncidentTypeView::id, Function.identity()));
        return reports.incidents(from, to).stream()
                .filter(row -> inScope(scope, row.residentId(), row.floorAreaId(), row.createdAt()))
                .map(row -> row.view(Optional.ofNullable(types.get(row.currentTypeId()))
                        .map(IncidentTypeView::nonHealth)
                        .orElse(false)))
                .toList()
                .stream();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonitoringView> underMonitoring(ScopeFilter scope) {
        var result = new ArrayList<MonitoringView>();
        for (var window : falls.activeWindows()) {
            if (window.residentId() == null) continue;
            var floor = currentFloor(window.residentId());
            if (inCurrentScope(scope, window.residentId(), floor)) {
                result.add(new MonitoringView(
                        window.residentId(),
                        POST_FALL,
                        floor,
                        window.content().anchor(),
                        window.content().endsAt()));
            }
        }
        for (var row : contacts.allMonitored()) {
            var content = row.content();
            if (content.monitoredFrom() == null) continue;
            Instant endsAt =
                    content.excludedAt() != null && content.excludedAt().isBefore(content.monitoredTo())
                            ? content.excludedAt()
                            : content.monitoredTo();
            var floor = currentFloor(row.residentId());
            if (inCurrentScope(scope, row.residentId(), floor)) {
                result.add(new MonitoringView(row.residentId(), CONTACT, floor, content.monitoredFrom(), endsAt));
            }
        }
        return List.copyOf(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> criticalResidentIds(ScopeFilter scope) {
        return critical.openFlags().stream()
                .map(flag -> flag.residentId())
                .filter(resident -> resident != null && inCurrentScope(scope, resident, currentFloor(resident)))
                .distinct()
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int cancelledCount(ScopeFilter scope, Instant from, Instant to) {
        return (int) reports.cancelledIncidents(from, to).stream()
                .filter(row -> inScope(scope, row.residentId(), row.floorAreaId(), row.createdAt()))
                .count();
    }

    /**
     * Vùng đang khoanh có tầng hoặc khu trong phạm vi. Phạm vi chỉ theo phòng hoặc người cao tuổi không thấy vùng (vùng
     * khoanh theo tầng, khu; 016 lọc tầng của người xem).
     */
    @Override
    @Transactional(readOnly = true)
    public List<IsolationZoneView> containmentZones(ScopeFilter scope) {
        return isolation.activeZones().stream()
                .filter(zone -> scope.facilityWide()
                        || zone.floorAreaIds().stream().anyMatch(scope.floorAreaIds()::contains)
                        || zone.areaIds().stream().anyMatch(scope.floorAreaIds()::contains))
                .toList();
    }

    private boolean inScope(ScopeFilter scope, Long residentId, Long rowFloor, Instant at) {
        if (scope.facilityWide()) return true;
        if (residentId == null) return rowFloor != null && scope.floorAreaIds().contains(rowFloor);
        if (scope.residentIds().contains(residentId)) return true;
        var location = locations.at(residentId, at);
        Long floor = location.map(LocationView::floorId).orElse(rowFloor);
        Long room = location.map(LocationView::roomId).orElse(null);
        return (floor != null && scope.floorAreaIds().contains(floor))
                || (room != null && scope.roomIds().contains(room));
    }

    private boolean inCurrentScope(ScopeFilter scope, long residentId, Long floor) {
        if (scope.facilityWide() || scope.residentIds().contains(residentId)) return true;
        if (floor != null && scope.floorAreaIds().contains(floor)) return true;
        return locations
                .current(residentId)
                .map(LocationView::roomId)
                .map(room -> scope.roomIds().contains(room))
                .orElse(false);
    }

    private Long currentFloor(long residentId) {
        return locations.current(residentId).map(LocationView::floorId).orElse(null);
    }
}
