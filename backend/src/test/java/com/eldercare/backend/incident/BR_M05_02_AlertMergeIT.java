package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.api.events.AlertCreated;
import com.eldercare.backend.incident.api.events.AlertLevelRaised;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import org.junit.jupiter.api.Test;

/** Gộp cảnh báo cùng khóa loại (BR-M05-02, FR-030, US3 kịch bản 4, 5, 8). */
class BR_M05_02_AlertMergeIT extends AlertRequestTestBase {
    private static final String KEY = "VUOT_NGUONG:9";

    @Test
    void BR_M05_02_sameKeyWhileOpenMergesAndAddsSource() {
        var first = request(KEY, IncidentLevel.MEDIUM, T0, 1);
        var second = request(KEY, IncidentLevel.MEDIUM, T0.plusSeconds(7200), 2);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.occurrenceCount()).isEqualTo(2);
        var stored = alerts.findById(first.id()).orElseThrow();
        assertThat(stored.latestAt()).isEqualTo(T0.plusSeconds(7200));
        assertThat(stored.firstAt()).isEqualTo(T0);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind = 'M05.ALERT_SOURCE' AND entity_id = ?",
                        Integer.class,
                        first.id()))
                .isEqualTo(2);
        assertThat(published).filteredOn(AlertCreated.class::isInstance).hasSize(1);
    }

    @Test
    void FR_030_higherLevelOccurrenceRaisesAlertLevel() {
        var first = request(KEY, IncidentLevel.MEDIUM, T0, 1);
        request(KEY, IncidentLevel.CRITICAL, T0.plusSeconds(60), 2);

        var stored = alerts.findById(first.id()).orElseThrow();
        assertThat(stored.level()).isEqualTo(IncidentLevel.CRITICAL);
        assertThat(stored.acknowledgementDeadline()).isNull();
        assertThat(published).filteredOn(AlertLevelRaised.class::isInstance).hasSize(1);
    }

    @Test
    void FR_030_closedAlertIsNotReusedSoNewOccurrenceCreatesNewAlert() {
        var first = request(KEY, IncidentLevel.MEDIUM, T0, 1);
        jdbc.update(
                "UPDATE alert SET status = 'CLOSED', closed_at = ? WHERE id = ?",
                java.sql.Timestamp.from(T0.plusSeconds(60)),
                first.id());

        var next = request(KEY, IncidentLevel.MEDIUM, T0.plusSeconds(120), 2);

        assertThat(next.id()).isNotEqualTo(first.id());
        assertThat(next.occurrenceCount()).isEqualTo(1);
    }

    @Test
    void FR_028_differentKeyCreatesSeparateAlert() {
        var first = request(KEY, IncidentLevel.MEDIUM, T0, 1);
        var other = request("VUOT_NGUONG:10", IncidentLevel.MEDIUM, T0, 2);

        assertThat(other.id()).isNotEqualTo(first.id());
    }
}
