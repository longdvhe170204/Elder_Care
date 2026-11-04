package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Diễn biến sự cố ghi vào bảng sự kiện chung, chỉ thêm (R-15, nhóm 3). */
@Transactional
class IncidentProgressionStoreIT extends MySqlIntegrationTestBase {
    @Autowired
    IncidentProgressionStore progressions;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_045_progressionsAreAppendedAndWishCheckReadsOwnIncidentOnly() {
        long incident = TestUsers.nextId();
        var at = Instant.parse("2026-11-04T02:00:00Z");

        long first = progressions.add(incident, 5L, "ACTION_TAKEN", "Sơ cứu", at, TestUsers.nextId(), false, 7L, at);
        long second =
                progressions.add(incident, 5L, "ACTION_TAKEN", "Gọi bác sĩ", at, TestUsers.nextId(), true, 7L, at);

        assertThat(second).isNotEqualTo(first);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind = 'M05.INCIDENT_PROGRESSION' AND entity_type = 'M05.INCIDENT' AND entity_id = ?",
                        Integer.class,
                        incident))
                .isEqualTo(2);
        assertThat(progressions.wishChecked(incident)).isFalse();
        assertThat(progressions.typeIds(incident)).containsExactly(5L, 5L);
    }

    @Test
    void FR_041_joinIsRecordedOncePerPerson() {
        long incident = TestUsers.nextId();
        long staff = TestUsers.nextId();
        var at = Instant.parse("2026-11-04T04:00:00Z");

        progressions.join(incident, staff, at, 7L);
        progressions.join(incident, staff, at, 7L);

        assertThat(progressions.isHandler(incident, staff)).isTrue();
        assertThat(progressions.isHandler(incident, TestUsers.nextId())).isFalse();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind = 'M05.INCIDENT_HANDLER' AND entity_id = ?",
                        Integer.class,
                        incident))
                .isEqualTo(1);
    }
}
