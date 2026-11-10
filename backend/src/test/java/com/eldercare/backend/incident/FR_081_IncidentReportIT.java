package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.IncidentStatisticsApi;
import com.eldercare.backend.incident.api.dto.AlertReportView;
import com.eldercare.backend.incident.api.dto.IncidentReportView;
import com.eldercare.backend.incident.api.dto.IsolationZoneView;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 007 T108 (FR-081; 016 FR-042, FR-042a, FR-020; "Đổi api" duyệt 2026-11-08): dòng sự cố cho 016 có mức lúc tiếp nhận
 * (giữ nguyên khi mức đổi sau đó; rỗng khi chưa tiếp nhận) và trạng thái hiện hành; dòng cảnh báo có trạng thái; số sự
 * cố Đã hủy (dòng tham khảo của FR-042) và vùng đang khoanh theo phạm vi.
 */
class FR_081_IncidentReportIT extends IncidentHttpTestBase {

    @Autowired
    IncidentStatisticsApi statistics;

    @Autowired
    ContainmentStore containments;

    private ScopeFilter floorScope() {
        return new ScopeFilter(false, Set.of(floor), Set.of(), Set.of());
    }

    @Test
    void FR_081_levelAtAcknowledgement_keptAfterLevelChange_andStatusIsCurrent() throws Exception {
        long received = incident("OTHER_INJURY", "LOW", "NEW", null, nurse.accountId());
        long waiting = incident("OTHER_INJURY", "MEDIUM", "NEW", null, nurse.accountId());
        post(floorManager, "/api/v1/incidents/{id}/receive", received, "{}").andExpect(status().isOk());
        post(
                        floorManager,
                        "/api/v1/incidents/{id}/change-level",
                        received,
                        "{\"newLevel\":\"MEDIUM\",\"reason\":\"Đau tăng\"}")
                .andExpect(status().isOk());

        Map<Long, IncidentReportView> rows = statistics
                .incidents(
                        new ScopeFilter(true, Set.of(), Set.of(), Set.of()),
                        time.now().minus(Duration.ofHours(1)),
                        time.now().plus(Duration.ofHours(1)))
                .collect(Collectors.toMap(IncidentReportView::incidentId, Function.identity()));

        assertThat(rows.get(received).level()).isEqualTo("MEDIUM");
        assertThat(rows.get(received).levelAtAcknowledgement()).isEqualTo("LOW");
        assertThat(rows.get(received).status()).isEqualTo("IN_PROGRESS");
        assertThat(rows.get(waiting).levelAtAcknowledgement()).isNull();
        assertThat(rows.get(waiting).status()).isEqualTo("NEW");
    }

    @Test
    void FR_081_alertRow_hasCurrentStatus() {
        var at = Timestamp.from(time.now());
        jdbc.update("""
                INSERT INTO alert (resident_id, floor_area_id, type, type_lock, level, first_at, latest_at, status)
                VALUES (?, ?, 'MANUAL', ?, 'LOW', ?, ?, 'RECEIVED')
                """, resident, floor, "T108-" + TestUsers.nextId(), at, at);
        long alert = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        AlertReportView row = statistics
                .alerts(
                        floorScope(),
                        time.now().minus(Duration.ofHours(1)),
                        time.now().plus(Duration.ofHours(1)))
                .filter(r -> r.alertId() == alert)
                .findFirst()
                .orElseThrow();

        assertThat(row.status()).isEqualTo("RECEIVED");
    }

    @Test
    void FR_042_cancelledIncidents_areCountedInScopeOnly() {
        incident("OTHER_INJURY", "LOW", "CANCELLED", null, nurse.accountId());
        incident("OTHER_INJURY", "LOW", "NEW", null, nurse.accountId());
        var from = time.now().minus(Duration.ofHours(1));
        var to = time.now().plus(Duration.ofHours(1));
        var otherFloor = new ScopeFilter(false, Set.of(TestUsers.nextId()), Set.of(), Set.of());

        assertThat(statistics.cancelledCount(floorScope(), from, to)).isEqualTo(1);
        assertThat(statistics.cancelledCount(otherFloor, from, to)).isZero();
    }

    @Test
    void FR_020_containmentZones_activeZonesTouchingTheScope() {
        long inScope = containments
                .create(
                        new ContainmentScope(Set.of(floor), Set.of()),
                        time.now(),
                        nurse.accountId(),
                        "Nghi nhiễm",
                        Set.of())
                .id();
        long elsewhere = containments
                .create(
                        new ContainmentScope(Set.of(TestUsers.nextId()), Set.of()),
                        time.now(),
                        nurse.accountId(),
                        "Nghi nhiễm",
                        Set.of())
                .id();

        assertThat(statistics.containmentZones(floorScope()).stream().map(IsolationZoneView::id))
                .contains(inScope)
                .doesNotContain(elsewhere);
    }
}
