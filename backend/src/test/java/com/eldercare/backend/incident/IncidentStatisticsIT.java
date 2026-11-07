package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.incident.api.dto.AlertReportView;
import com.eldercare.backend.incident.api.dto.IncidentReportView;
import com.eldercare.backend.incident.internal.critical.domain.CriticalFlagContent;
import com.eldercare.backend.incident.internal.critical.domain.CriticalFlagStatus;
import com.eldercare.backend.incident.internal.critical.domain.CriticalFlagType;
import com.eldercare.backend.incident.internal.critical.persistence.CriticalCareStore;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Nguồn thống kê của 016 (FR-081; module-interfaces mục 1.10; T061): cảnh báo kèm nguồn SOS, nút hỗ trợ, mốc tới nơi;
 * sự cố bỏ Đã hủy, kèm dấu "không thuộc sức khỏe"; khoảng đang theo dõi; người đang nguy kịch; lọc theo phạm vi.
 */
class IncidentStatisticsIT extends IncidentHttpTestBase {
    private static final ScopeFilter EMPTY = new ScopeFilter(false, Set.of(), Set.of(), Set.of());

    @Autowired
    IncidentStatisticsApi statistics;

    @Autowired
    FallMonitoringStore falls;

    @Autowired
    CriticalCareStore critical;

    @Test
    void FR_081_alertsCarrySourceAcknowledgementAndArrival_filteredByScope() {
        long sos = alert(resident, "RESIDENT_HELP_REQUEST", "CRITICAL");
        jdbc.update(
                "UPDATE alert SET received_at = ?, level_at_received = 'CRITICAL', arrived_at = ? WHERE id = ?",
                Timestamp.from(time.now().plusSeconds(60)),
                Timestamp.from(time.now().plusSeconds(120)),
                sos);
        long assist = alert(null, "STAFF_ASSIST_REQUEST", "CRITICAL");
        jdbc.update("UPDATE alert SET skip_arrival_time = TRUE WHERE id = ?", assist);
        long vital = alert(resident, "VITAL_SIGN", "MEDIUM");

        Map<Long, AlertReportView> all = statistics
                .alerts(ScopeFilter.FACILITY, from(), to())
                .collect(Collectors.toMap(AlertReportView::alertId, Function.identity()));

        assertThat(all.get(sos).source()).isEqualTo("SOS");
        assertThat(all.get(sos).levelAtAcknowledgement()).isEqualTo("CRITICAL");
        assertThat(all.get(sos).acknowledgedAt()).isNotNull();
        assertThat(all.get(sos).arrivedAt()).isNotNull();
        assertThat(all.get(assist).source()).isEqualTo("EMERGENCY_ASSIST");
        assertThat(all.get(assist).arrivalTimeMissing()).isTrue();
        assertThat(all.get(vital).source()).isEqualTo("OTHER");

        var byResident = new ScopeFilter(false, Set.of(), Set.of(), Set.of(resident));
        assertThat(statistics.alerts(byResident, from(), to()).map(AlertReportView::alertId))
                .contains(sos, vital)
                .doesNotContain(assist);
        var byFloor = new ScopeFilter(false, Set.of(floor), Set.of(), Set.of());
        assertThat(statistics.alerts(byFloor, from(), to()).map(AlertReportView::alertId))
                .contains(sos, assist, vital);
        assertThat(statistics.alerts(EMPTY, from(), to()).map(AlertReportView::alertId))
                .doesNotContain(sos, assist, vital);
        assertThat(statistics
                        .alerts(ScopeFilter.FACILITY, to(), to().plusSeconds(60))
                        .map(AlertReportView::alertId))
                .doesNotContain(sos, assist, vital);
    }

    @Test
    void FR_081_incidentsSkipCancelled_markNonHealthType() {
        long fall = incident("FALL", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        long facility = incident("FACILITY", "LOW", "NEW", null, nurse.accountId());
        long cancelled = incident("FALL", "LOW", "CANCELLED", null, nurse.accountId());

        Map<Long, IncidentReportView> rows = statistics
                .incidents(new ScopeFilter(false, Set.of(floor), Set.of(), Set.of()), from(), to())
                .collect(Collectors.toMap(IncidentReportView::incidentId, Function.identity()));

        assertThat(rows).containsKeys(fall, facility).doesNotContainKey(cancelled);
        assertThat(rows.get(fall).nonHealth()).isFalse();
        assertThat(rows.get(fall).level()).isEqualTo("MEDIUM");
        assertThat(rows.get(fall).type()).isEqualTo("FALL");
        assertThat(rows.get(facility).nonHealth()).isTrue();
    }

    @Test
    void FR_081_monitoringAndCriticalResidents_byScope() {
        long fall = incident("FALL", "MEDIUM", "MONITORING", nurse.accountId(), nurse.accountId());
        Instant endsAt = time.now().plus(Duration.ofHours(24));
        falls.start(resident, fall, time.now(), endsAt, Duration.ofHours(2), nurse.accountId());
        long other = TestUsers.nextId();
        critical.create(
                other,
                CriticalFlagStatus.OPEN,
                new CriticalFlagContent(
                        CriticalFlagType.OFFICIAL,
                        "Suy hô hấp",
                        nurse.accountId(),
                        time.now(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                nurse.accountId());

        var mine = new ScopeFilter(false, Set.of(), Set.of(), Set.of(resident));
        assertThat(statistics.underMonitoring(mine)).anySatisfy(view -> {
            assertThat(view.residentId()).isEqualTo(resident);
            assertThat(view.type()).isEqualTo("POST_FALL");
            assertThat(view.endsAt()).isEqualTo(endsAt);
        });
        assertThat(statistics.criticalResidentIds(mine)).doesNotContain(other);
        assertThat(statistics.criticalResidentIds(ScopeFilter.FACILITY)).contains(other);
        assertThat(statistics.underMonitoring(EMPTY)).noneMatch(view -> view.residentId() == resident);
    }

    private long alert(Long residentId, String type, String level) {
        long id = TestUsers.nextId();
        var at = Timestamp.from(time.now());
        jdbc.update("""
                INSERT INTO alert (id, resident_id, floor_area_id, type, type_lock, level, first_at, latest_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, residentId, floor, type, "TEST:" + id, level, at, at);
        return id;
    }

    private Instant from() {
        return time.now().minus(Duration.ofMinutes(1));
    }

    private Instant to() {
        return time.now().plus(Duration.ofMinutes(1));
    }
}
