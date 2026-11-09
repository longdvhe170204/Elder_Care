package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.dto.AccountView;
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
    private final AccountApi accounts;

    public DefaultIncidentApi(
            IncidentStore incidents,
            AlertStore alerts,
            ContactPersonStore contacts,
            FallMonitoringStore fallMonitoring,
            CriticalCareStore critical,
            AccountApi accounts) {
        this.incidents = incidents;
        this.alerts = alerts;
        this.contacts = contacts;
        this.fallMonitoring = fallMonitoring;
        this.critical = critical;
        this.accounts = accounts;
    }

    @Override
    public Optional<IncidentView> view(long incidentId) {
        return incidents.sourceView(incidentId).map(this::published);
    }

    @Override
    public List<IncidentView> byResident(long residentId, Instant from, Instant to, boolean excludeVoided) {
        return incidents.sourceByResident(residentId, from, to, excludeVoided).stream()
                .map(this::published)
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

    /** Đã hủy lưu {@code CANCELLED} trả {@code VOIDED}; người xử lý đổi từ tài khoản sang mã nhân viên (013 FR-011a). */
    private IncidentView published(IncidentView row) {
        Long assigneeStaffId = incidents
                .assignee(row.id())
                .flatMap(accounts::byId)
                .map(AccountView::staffId)
                .orElse(null);
        String status = STORED_CANCELLED.equals(row.status()) ? VOIDED : row.status();
        return new IncidentView(
                row.id(),
                row.residentId(),
                row.type(),
                row.level(),
                status,
                row.createdAt(),
                row.floorAreaId(),
                assigneeStaffId);
    }
}
