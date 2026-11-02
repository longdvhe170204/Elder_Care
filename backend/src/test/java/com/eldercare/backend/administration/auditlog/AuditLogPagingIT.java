package com.eldercare.backend.administration.auditlog;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** API đọc được E bổ sung theo duyệt 2026-10-25; không thay đổi API đọc cũ của A. */
class AuditLogPagingIT extends MySqlIntegrationTestBase {
    @Autowired
    AuditLogApi audit;

    @Autowired
    JdbcTemplate jdbc;

    private final Instant from = Instant.parse("2026-10-25T00:00:00Z");

    private void row(String type, Instant at, String actor, Long id) {
        jdbc.update(
                "INSERT INTO audit_log(occurred_at,actor_type,actor_id,role,action,entity_type,entity_id,details) VALUES (?,?,?,'CAREGIVER','PAGING_TEST',?,'1',JSON_OBJECT('basis','SHIFT_LEAD'))",
                Timestamp.from(at),
                actor,
                id,
                type);
    }

    @Test
    void FR_056_keysetReturnsEveryRowBeyond200_evenWhenTimesAreNotIdOrdered() {
        String type = "M09.PAGING_" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 405; i++) row(type, from.plusSeconds(405 - i), "USER", 123L);
        row(type, from.minusSeconds(1), "USER", 123L);
        row(type, from.plusSeconds(500), "USER", 123L);
        row(type + "_OTHER", from, "USER", 123L);
        var all = new ArrayList<AuditLogRecord>();
        long after = 0;
        List<AuditLogRecord> page;
        do {
            page = audit.recordedBetween(type, from, from.plusSeconds(500), after, 200);
            assertThat(page).hasSizeLessThanOrEqualTo(200);
            all.addAll(page);
            if (!page.isEmpty()) after = page.getLast().id();
        } while (page.size() == 200);
        assertThat(all).hasSize(405);
        assertThat(all).extracting(AuditLogRecord::id).isSorted().doesNotHaveDuplicates();
        assertThat(all.getFirst()).satisfies(r -> {
            assertThat(r.actorAccountId()).isEqualTo(123);
            assertThat(r.details()).containsEntry("basis", "SHIFT_LEAD");
        });
        assertThat(audit.recorded(new AuditAction("PAGING_TEST", false), type, null, null))
                .hasSize(200);
    }

    @Test
    void FR_056_periodIsHalfOpen_andNonUserActorHasNoAccount() {
        String type = "M09.PAGING_" + UUID.randomUUID().toString().substring(0, 8);
        row(type, from, "SCHEDULER", null);
        row(type, from.plusSeconds(1), "USER", 123L);
        var page = audit.recordedBetween(type, from, from.plusSeconds(1), 0, 1);
        assertThat(page).hasSize(1);
        assertThat(page.getFirst().occurredAt()).isEqualTo(from);
        assertThat(page.getFirst().actorAccountId()).isNull();
    }

    @Test
    void FR_056_invalidPeriodCursorAndPageSizeAreRejected() {
        assertThatThrownBy(() -> audit.recordedBetween("M09.HANDOVER", from, from, 0, 200))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> audit.recordedBetween("M09.HANDOVER", from, from.plusSeconds(1), -1, 200))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> audit.recordedBetween("M09.HANDOVER", from, from.plusSeconds(1), 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> audit.recordedBetween("M09.HANDOVER", from, from.plusSeconds(1), 0, 201))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
