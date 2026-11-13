package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.api.events.AlertCreated;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import org.junit.jupiter.api.Test;

/** Gộp cảnh báo cùng khóa loại (BR-M05-02, FR-030, US3 kịch bản 4, 5, 8). */
class BR_41_AlertMergeIT extends AlertRequestTestBase {
    private static final String KEY = "VUOT_NGUONG:9";

    @Test
    void BR_41_sameKeyWhileOpenMergesAndAddsSource() {
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
}
