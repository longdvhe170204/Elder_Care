package com.eldercare.backend.administration.retention;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.retention.scheduling.ExpiredDataPurgeJob;
import com.eldercare.backend.administration.sample.SampleFloorRecordProvider;
import com.eldercare.backend.administration.sample.SampleOfficeRecordProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
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
    private static final String REQUEST = "M15.APPROVAL_REQUEST";
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

    private String runJob() {
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

    private long request(String status, Instant at, Long residentId) {
        LocalDateTime time = LocalDateTime.ofInstant(at, ZoneOffset.UTC);
        jdbc.update(
                "INSERT INTO approval_request (type, status, content, resident_id, requester_id, created_at, reason,"
                        + " requested_effective_date, decided_at) VALUES ('M15.SAMPLE', ?, '{}', ?, ?, ?, 'đề nghị', ?, ?)",
                status,
                residentId,
                NURSE.accountId(),
                time,
                time.toLocalDate(),
                status.equals("PENDING_APPROVAL") ? null : time);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
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

    private Map<String, Object> summary(String runId) {
        return jdbc.queryForMap(
                "SELECT actor_type, exempt_from_purge,"
                        + " JSON_EXTRACT(basis, '$.schedulerRunId') AS run_id,"
                        + " JSON_EXTRACT(details, '$.deleted.\"M15.SAMPLE_RECORD\".count') AS records,"
                        + " JSON_EXTRACT(details, '$.deleted.\"M15.AUDIT_LOG\".count') AS logs,"
                        + " JSON_EXTRACT(details, '$.parameters.\"CFG-M15-04\"') AS parameter,"
                        + " JSON_EXTRACT(details, '$.stopped') AS stopped"
                        + " FROM audit_log WHERE action = 'EXPIRED_DATA_PURGED' AND entity_id = ?",
                runId);
    }

    @Test
    void US3_4_BR_M15_12_expiredChain_isPurgedWithItsLogs_andSummaryIsWritten() {
        long old = floorRecord(expired);
        logFor(FLOOR, old, expired, null);
        long kept = floorRecord(recent);
        logFor(FLOOR, kept, recent, null);

        String runId = runJob();

        assertThat(exists("sample_floor_record", old)).isFalse();
        assertThat(logsOf(FLOOR, old)).isZero();
        assertThat(exists("sample_floor_record", kept)).isTrue();
        assertThat(logsOf(FLOOR, kept)).isEqualTo(1);
        Map<String, Object> summary = summary(runId);
        assertThat(summary).containsEntry("actor_type", "SCHEDULER").containsEntry("exempt_from_purge", true);
        assertThat(String.valueOf(summary.get("run_id"))).isEqualTo("\"" + runId + "\"");
        assertThat(String.valueOf(summary.get("records"))).isEqualTo("1");
        assertThat(String.valueOf(summary.get("logs"))).isEqualTo("1");
        assertThat(summary.get("parameter")).isNotNull();
        assertThat(String.valueOf(summary.get("stopped"))).isEqualTo("false");
    }

    /** Dòng bảng dùng chung của bản ghi (lịch sử, bản ghi con, đối tượng nhỏ, dấu đã nhắc), research R-15 mục 2. */
    private void sharedRowsFor(String type, long id) {
        jdbc.update(
                "INSERT INTO status_history (entity_type, entity_id, attribute, module, kind, value, starts_at,"
                        + " by_system) VALUES (?, ?, 'status', 'M15', 'POINT', 'A', UTC_TIMESTAMP(6), FALSE)",
                type,
                id);
        jdbc.update(
                "INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at,"
                        + " confirmed_at) VALUES (?, ?, 'M15', 'M15.TEST_EVENT', '{}', FALSE, UTC_TIMESTAMP(6),"
                        + " UTC_TIMESTAMP(6))",
                type,
                id);
        jdbc.update(
                "INSERT INTO simple_record (kind, module, entity_type, entity_id, status, content, created_at,"
                        + " created_by, version, locked_at) VALUES ('M15.TEST_RECORD', 'M15', ?, ?, 'DONE', '{}',"
                        + " UTC_TIMESTAMP(6), 1, 0, UTC_TIMESTAMP(6))",
                type,
                id);
        jdbc.update(
                "INSERT INTO reminder_marker (kind, entity_type, entity_id, marker, marked_at) VALUES"
                        + " ('M15.TEST', ?, ?, 'm', UTC_TIMESTAMP(6))",
                type,
                id);
    }

    private int sharedRowsOf(String type, long id) {
        int n = 0;
        for (String table : new String[] {"status_history", "record_event", "simple_record", "reminder_marker"}) {
            n += jdbc.queryForObject(
                    "SELECT COUNT(*) FROM " + table + " WHERE entity_type = ? AND entity_id = ?",
                    Integer.class,
                    type,
                    id);
        }
        return n;
    }

    private long sharedEventFor(String type, long id) {
        jdbc.update(
                "INSERT INTO record_event (entity_type, entity_id, module, kind, content, by_system, occurred_at,"
                        + " confirmed_at) VALUES (?, ?, 'M15', 'M15.TEST_EVENT', '{}', FALSE, UTC_TIMESTAMP(6),"
                        + " UTC_TIMESTAMP(6))",
                type,
                id);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
