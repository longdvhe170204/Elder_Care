package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import com.eldercare.backend.incident.internal.infection.persistence.ContactFollowUpStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Kiểm cổng 003 trên dữ liệu thật của 007, không thay bằng nguồn giả. */
@Transactional
class IncidentAllocationStatusIT extends MySqlIntegrationTestBase {
    private static final Instant START = Instant.parse("2026-11-03T00:00:00Z");

    @Autowired
    AllocationInfectionStatus infections;

    @Autowired
    ContainmentStore containments;

    @Autowired
    ContactFollowUpStore contacts;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void BR_M05_11_overlappingContainmentsStayBlockedUntilAllAreLifted() {
        long floor = TestUsers.nextId();
        long actor = TestUsers.nextId();
        var scope = new ContainmentScope(Set.of(floor), Set.of());
        var first = containments.create(scope, START, actor, "Khoanh vùng", Set.of(TestUsers.nextId()));
        var second = containments.create(scope, START, actor, "Khoanh vùng", Set.of(TestUsers.nextId()));
        var firstEnd = START.plusSeconds(60);
        var lastEnd = START.plusSeconds(120);

        containments.release(first.id(), first.version(), firstEnd, actor, "Gỡ vùng thứ nhất");
        assertThat(infections.cordonedAt(firstEnd).floorIds()).contains(floor);
        containments.release(second.id(), second.version(), lastEnd, actor, "Gỡ vùng thứ hai");
        assertThat(infections.cordonedAt(firstEnd).floorIds()).contains(floor);
        assertThat(infections.cordonedAt(lastEnd).floorIds()).doesNotContain(floor);
        assertThat(infections.cordonedAt(START.minusSeconds(1)).floorIds()).doesNotContain(floor);
    }

    @Test
    void BR_M03_10_disinfectionIsRequiredForSuspectedOrExposedResidents() {
        long suspected = TestUsers.nextId();
        long exposed = TestUsers.nextId();
        long unrelated = TestUsers.nextId();
        long incident = TestUsers.nextId();
        var end = START.plusSeconds(60);
        var at = Timestamp.from(START);
        jdbc.update("""
                INSERT INTO incident (id, resident_id, incident_type_id, current_type_id,
                  type_code, discovered_at, description, initial_level, level,
                  recorded_by, created_at, confirmed_at)
                VALUES (?, ?, ?, ?, 'SUSPECTED_INFECTION', ?, 'Nghi nhiễm', 'MEDIUM', 'MEDIUM', ?, ?, ?)
                """, incident, suspected, TestUsers.nextId(), TestUsers.nextId(), at, TestUsers.nextId(), at, at);
        contacts.add(TestUsers.nextId(), exposed, START, end, TestUsers.nextId());

        assertThat(infections.listedAt(suspected, START)).isTrue();
        assertThat(infections.listedAt(exposed, START)).isTrue();
        assertThat(infections.listedAt(unrelated, START)).isFalse();
        assertThat(infections.listedAt(suspected, START.minusSeconds(1))).isFalse();
        assertThat(infections.listedAt(exposed, end)).isFalse();

        jdbc.update("UPDATE incident SET status = 'CLOSED', closed_at = ? WHERE id = ?", Timestamp.from(end), incident);
        assertThat(infections.listedAt(suspected, end)).isFalse();
        assertThat(infections.listedAt(suspected, START)).isTrue();
    }
}
