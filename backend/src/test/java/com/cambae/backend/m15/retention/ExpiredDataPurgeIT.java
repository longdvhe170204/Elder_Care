package com.cambae.backend.m15.retention;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import com.cambae.backend.m15.retention.api.RetentionProvider.DataRef;
import com.cambae.backend.m15.retention.scheduling.ExpiredDataPurgeJob;
import com.cambae.backend.m15.sample.SampleFloorRecordProvider;
import com.cambae.backend.m15.sample.SampleOfficeRecordProvider;
import com.cambae.backend.support.FakePermissions;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * US3 #4, #5; BR-M15-12, BR-M15-13; FR-050, FR-051, FR-051a (000 T066, đồng hồ giả lập): job chỉ xóa chuỗi mà mọi phần
 * tử (bản gốc, bản tham chiếu tới nó, đính chính, nhật ký) đã hết hạn; mỗi chuỗi một transaction; dừng giữa chừng vẫn
 * ghi bản tóm tắt với số đếm thực tế, bản tóm tắt không bị loại bỏ; tài khoản ứng dụng không xóa được.
 */
@Import(RetentionFixtures.class)
class ExpiredDataPurgeIT extends MySqlIntegrationTestBase {

    private static final String FLOOR = SampleFloorRecordProvider.TYPE;
    private static final String OFFICE = SampleOfficeRecordProvider.TYPE;
    private static final AuditAction RECORDED = new AuditAction("SAMPLE_RECORDED", false);
    private static final CurrentUser NURSE = FakePermissions.user(10, Role.NURSING_STAFF);
    private static final CurrentUser SCHEDULER = FakePermissions.user(0, Role.SCHEDULER);

    @Autowired
    ExpiredDataPurgeJob job;

    @Autowired
    RetentionFixtures.Control control;

    @Autowired
    AuditLogApi auditLog;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    SwitchableUser user;

    @Autowired
    TestClockControl clock;

    @Autowired
    BusinessTime time;

    @Autowired
    JdbcTemplate jdbc;

    /** Quá CFG-M15-04 (mặc định 10 năm). */
    private Instant expired;

    /** Còn trong thời hạn. */
    private Instant recent;

    @BeforeEach
    void setUp() {
        control.clear();
        expired = time.now().atZone(BusinessTime.ZONE).minusYears(11).toInstant();
        recent = time.now().minusSeconds(86_400);
        runJob(); // dọn dữ liệu hết hạn còn lại từ test trước
    }

    @AfterEach
    void tearDown() {
        control.clear();
        clock.reset();
        user.set(NURSE);
    }

    private long runJob() {
        user.set(SCHEDULER);
        try {
            return job.run();
        } finally {
            user.set(NURSE);
        }
    }

    private long record(String table, Instant at) {
        String floor = table.equals("sample_floor_record") ? ", floor_area_id" : "";
        String floorValue = table.equals("sample_floor_record") ? ", 2" : "";
        jdbc.update(
                "INSERT INTO " + table + " (occurred_at, actor_id, note" + floor + ") VALUES (?, ?, 'ghi nhận'"
                        + floorValue + ")",
                LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                NURSE.accountId());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long floorRecord(Instant at) {
        return record("sample_floor_record", at);
    }

    private long officeRecord(Instant at) {
        return record("sample_office_record", at);
    }

    private void logFor(String type, long id, Instant at, Long residentId) {
        clock.setTo(at);
        user.set(NURSE);
        try {
            tx.executeWithoutResult(s -> auditLog.write(AuditLogEntry.builder()
                    .action(RECORDED)
                    .entity(type, id)
                    .resident(residentId)
                    .after(Map.of("note", "ghi nhận"))
                    .build()));
        } finally {
            clock.reset();
        }
    }

    private long correctionFor(String type, long id, Instant at) {
        jdbc.update(
                "INSERT INTO correction (record_type, original_record_id, type, corrected_content, reason, actor_id,"
                        + " creation_method, recorded_at, sequence_no) VALUES (?, ?, 'AMEND_CONTENT', '{\"note\":\"đúng\"}',"
                        + " 'ghi nhầm', ?, 'A', ?, 1)",
                type,
                id,
                NURSE.accountId(),
                LocalDateTime.ofInstant(at, ZoneOffset.UTC));
        long correctionId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update(
                "INSERT INTO correction_chain (record_type, original_record_id, correction_count) VALUES (?, ?, 1)",
                type,
                id);
        return correctionId;
    }

    private boolean exists(String table, long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id) > 0;
    }

    private int logsOf(String type, long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = ? AND entity_id = ?",
                Integer.class,
                type,
                String.valueOf(id));
    }

    private Map<String, Object> summary(long runId) {
        return jdbc.queryForMap(
                "SELECT actor_type, exempt_from_purge,"
                        + " JSON_EXTRACT(basis, '$.schedulerRunId') AS run_id,"
                        + " JSON_EXTRACT(details, '$.deleted.\"M15.SAMPLE_RECORD\".count') AS records,"
                        + " JSON_EXTRACT(details, '$.deleted.\"M15.AUDIT_LOG\".count') AS logs,"
                        + " JSON_EXTRACT(details, '$.parameters.\"CFG-M15-04\"') AS parameter,"
                        + " JSON_EXTRACT(details, '$.stopped') AS stopped"
                        + " FROM audit_log WHERE action = 'EXPIRED_DATA_PURGED' AND entity_id = ?",
                String.valueOf(runId));
    }

    @Test
    void US3_4_BR_M15_12_expiredChain_isPurgedWithItsLogs_andSummaryIsWritten() {
        long old = floorRecord(expired);
        logFor(FLOOR, old, expired, null);
        long kept = floorRecord(recent);
        logFor(FLOOR, kept, recent, null);

        long runId = runJob();

        assertThat(exists("sample_floor_record", old)).isFalse();
        assertThat(logsOf(FLOOR, old)).isZero();
        assertThat(exists("sample_floor_record", kept)).isTrue();
        assertThat(logsOf(FLOOR, kept)).isEqualTo(1);
        Map<String, Object> summary = summary(runId);
        assertThat(summary).containsEntry("actor_type", "SCHEDULER").containsEntry("exempt_from_purge", true);
        assertThat(String.valueOf(summary.get("run_id"))).isEqualTo(String.valueOf(runId));
        assertThat(String.valueOf(summary.get("records"))).isEqualTo("1");
        assertThat(String.valueOf(summary.get("logs"))).isEqualTo("1");
        assertThat(summary.get("parameter")).isNotNull();
        assertThat(String.valueOf(summary.get("stopped"))).isEqualTo("false");
    }

    @Test
    void FR_050_logOfResidentRecord_isKept_untilResidentRetentionIsKnown() {
        long linked = floorRecord(expired);
        logFor(FLOOR, linked, expired, 99L);
        logFor("M15.SAMPLE_PROFILE", 99L, expired, 99L);

        runJob();

        // Hạn của hồ sơ tính từ ngày kết thúc lưu trú (001); chưa có thì giữ (plan 000, D5)
        assertThat(exists("sample_floor_record", linked)).isTrue();
        assertThat(logsOf(FLOOR, linked)).isEqualTo(1);
        assertThat(logsOf("M15.SAMPLE_PROFILE", 99L)).isPositive();
    }

    @Test
    void US3_5_FR_051a_unexpiredCorrection_keepsChain_expiredCorrection_isPurgedWithChain() {
        long withRecent = floorRecord(expired);
        long recentCorrection = correctionFor(FLOOR, withRecent, recent);
        long withOld = floorRecord(expired);
        long oldCorrection = correctionFor(FLOOR, withOld, expired);

        runJob();

        assertThat(exists("sample_floor_record", withRecent)).isTrue();
        assertThat(exists("correction", recentCorrection)).isTrue();
        assertThat(exists("sample_floor_record", withOld)).isFalse();
        assertThat(exists("correction", oldCorrection)).isFalse();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM correction_chain WHERE record_type = ? AND original_record_id = ?",
                        Integer.class,
                        FLOOR,
                        withOld))
                .isZero();
    }

    @Test
    void FR_051a_recordReferencedByUnexpiredRecord_isKept_bothExpired_arePurgedTogether() {
        long target = floorRecord(expired);
        long recentReferrer = officeRecord(recent);
        control.reference(new DataRef(OFFICE, recentReferrer), new DataRef(FLOOR, target));
        long target2 = floorRecord(expired);
        long oldReferrer = officeRecord(expired);
        control.reference(new DataRef(OFFICE, oldReferrer), new DataRef(FLOOR, target2));

        runJob();

        assertThat(exists("sample_floor_record", target)).isTrue();
        assertThat(exists("sample_office_record", recentReferrer)).isTrue();
        assertThat(exists("sample_floor_record", target2)).isFalse();
        assertThat(exists("sample_office_record", oldReferrer)).isFalse();
    }

    @Test
    void BR_M15_13_interruptedRun_keepsUnfinishedChainWhole_andSummaryCountsActual() {
        long first = floorRecord(expired);
        logFor(FLOOR, first, expired, null);
        long second = floorRecord(expired);
        logFor(FLOOR, second, expired, null);
        control.failOnDelete(second);

        long runId = runJob();

        assertThat(exists("sample_floor_record", first)).isFalse();
        assertThat(exists("sample_floor_record", second)).isTrue();
        assertThat(logsOf(FLOOR, second)).isEqualTo(1);
        Map<String, Object> summary = summary(runId);
        assertThat(String.valueOf(summary.get("records"))).isEqualTo("1");
        assertThat(String.valueOf(summary.get("stopped"))).isEqualTo("true");
        assertThat(jdbc.queryForObject("SELECT result FROM scheduler_run WHERE id = ?", String.class, runId))
                .isEqualTo("FAILED");
    }

    @Test
    void FR_051_appAccountCannotDelete_retentionCannotDeleteSummary() throws SQLException {
        long record = floorRecord(recent);
        logFor(FLOOR, record, recent, null);
        long runId = runJob();

        assertThatThrownBy(() -> jdbc.update("DELETE FROM sample_floor_record WHERE id = ?", record))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM audit_log WHERE entity_id = ?", String.valueOf(record)))
                .isInstanceOf(DataAccessException.class);
        try (Connection c = retentionConnection();
                Statement s = c.createStatement()) {
            assertThatThrownBy(() -> s.executeUpdate(
                            "DELETE FROM audit_log WHERE action = 'EXPIRED_DATA_PURGED' AND entity_id = '" + runId
                                    + "'"))
                    .isInstanceOf(SQLException.class);
        }
    }
}
