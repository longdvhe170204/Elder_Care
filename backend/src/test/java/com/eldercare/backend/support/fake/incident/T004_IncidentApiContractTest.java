package com.eldercare.backend.support.fake.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.incident.api.dto.*;
import com.eldercare.backend.incident.api.events.CallReminderStopRequested;
import com.eldercare.backend.incident.api.events.WishReconfirmationCompleted;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Hợp đồng nguồn M05 cho 009, 012, 013 và 016; không thay kiểm thử publisher thật. */
class T004_IncidentApiContractTest {
    private final Instant from = Instant.parse("2026-11-01T00:00:00Z");
    private final Instant to = from.plusSeconds(3600);

    @Test
    void FR_083_openItemIncludesAlertsFromTheSharedFakeConfiguration() {
        // Hợp đồng 1.7, S-17: điều kiện KT_D xét cả cảnh báo và sự cố còn mở.
        try (var context = new AnnotationConfigApplicationContext(FakeM05Config.class)) {
            var incidents = context.getBean(FakeIncidents.class);
            var alerts = context.getBean(FakeAlerts.class);
            alerts.put(new AlertView(1, 10L, "VITAL_SIGN", "LOW", "NEW", from, 20L));
            assertThat(incidents.hasOpenItem(10)).isTrue();
            assertThat(incidents.hasOpenItem(11)).isFalse();

            alerts.put(new AlertView(1, 10L, "VITAL_SIGN", "LOW", "CLOSED", from, 20L));
            assertThat(incidents.hasOpenItem(10)).isFalse();
            incidents.put(new IncidentView(2, 10L, "FALL", "LOW", "MONITORING", from, 20L));
            assertThat(incidents.hasOpenItem(10)).isTrue();
        }
    }

    @Test
    void FR_040_historyIncludesClosedIncidentsAndUsesHalfOpenRange() {
        var fake = new FakeIncidents(new FakeAlerts());
        fake.put(new IncidentView(1, 10L, "FALL", "LOW", "CLOSED", from, 20L));
        fake.put(new IncidentView(2, 10L, "FALL", "LOW", "VOIDED", from, 20L));
        fake.put(new IncidentView(3, 10L, "FALL", "LOW", "NEW", to, 20L));
        fake.put(new IncidentView(4, 11L, "FALL", "LOW", "NEW", from, 20L));
        assertThat(fake.byResident(10, from, to, true))
                .extracting(IncidentView::id)
                .containsExactly(1L);
        assertThat(fake.byResident(10, from, to, false)).hasSize(2);
        assertThat(fake.hasOpenItem(10)).isTrue();
        assertThat(fake.view(99)).isEmpty();
    }

    @Test
    void FR_081_statisticsNeverFallBackToFacilityDataForAnUnconfiguredScope() {
        var fake = new FakeIncidentStatistics();
        var row = new IncidentReportView(1, 10L, "LOW", "FALL", false, 20L, from, null, 0);
        fake.incidents(ScopeFilter.FACILITY, List.of(row));
        var limited = new ScopeFilter(false, Set.of(21L), Set.of(), Set.of());
        assertThat(fake.incidents(limited, from, to)).isEmpty();
        assertThat(fake.incidents(ScopeFilter.FACILITY, from, to)).containsExactly(row);
        assertThat(fake.incidents(ScopeFilter.FACILITY, from.minusSeconds(1), from))
                .isEmpty();
    }

    @Test
    void FR_082_familyHealthIsConfiguredForTheExactResidentAndFamilyPair() {
        var fake = new FakeFamilyPortalHealth();
        var row = new FamilyIncidentView(
                1, new FamilyIncidentGeneralPart("Ngã", from, "LOW", "CLOSED"), Optional.empty());
        fake.incidents(10, 30, List.of(row));
        assertThat(fake.incidents(10, 30, from, to)).containsExactly(row);
        assertThat(fake.incidents(10, 31, from, to)).isEmpty();
        assertThat(fake.incidents(11, 30, from, to)).isEmpty();
        assertThat(row.health()).isEmpty();
    }

    @Test
    void FR_026a_alertAndIncidentAcknowledgementsHaveDistinctTargets() {
        var fake = new FakeIncidentCommands();
        fake.acknowledge(1, null);
        fake.acknowledgeAlert(2, null);
        assertThat(fake.acknowledgedIncidents()).containsExactly(1L);
        assertThat(fake.acknowledgedAlerts()).containsExactly(2L);
    }

    @Test
    void BR_M05_22_stopAndReconfirmationEventsKeepTheSourceIdentity() {
        var stop = new CallReminderStopRequested("M05.INCIDENT", 12);
        var completed = new WishReconfirmationCompleted(13);
        assertThat(stop.sourceRecordType()).isEqualTo("M05.INCIDENT");
        assertThat(stop.sourceRecordId()).isEqualTo(12);
        assertThat(completed.alertId()).isEqualTo(13);
    }
}
