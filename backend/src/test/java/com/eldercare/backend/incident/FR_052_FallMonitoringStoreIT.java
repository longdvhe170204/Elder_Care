package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Mỗi người tối đa một khoảng theo dõi sau ngã đang diễn ra; lần ngã mới thay khoảng cũ (FR-052). */
@Transactional
class FR_052_FallMonitoringStoreIT extends MySqlIntegrationTestBase {
    @Autowired
    FallMonitoringStore store;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_052_newFallReplacesActiveWindow() {
        long resident = TestUsers.nextId();
        var first = Instant.parse("2026-11-04T05:00:00Z");
        long firstId = store.start(
                resident, TestUsers.nextId(), first, first.plus(Duration.ofHours(72)), Duration.ofHours(4), 900L);
        var second = Instant.parse("2026-11-05T08:00:00Z");
        long secondIncident = TestUsers.nextId();

        long secondId = store.start(
                resident, secondIncident, second, second.plus(Duration.ofHours(72)), Duration.ofHours(4), 900L);

        var active = store.active(resident).orElseThrow();
        assertThat(active.id()).isEqualTo(secondId);
        assertThat(active.content().incidentId()).isEqualTo(secondIncident);
        assertThat(active.content().anchor()).isEqualTo(second);
        assertThat(active.content().intervalMinutes()).isEqualTo(240);
        assertThat(jdbc.queryForObject("SELECT status FROM simple_record WHERE id = ?", String.class, firstId))
                .isEqualTo(FallMonitoringStore.REPLACED);
    }
}
