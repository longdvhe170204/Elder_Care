package com.eldercare.backend.administration.auditlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.Basis;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-047, FR-045, FR-046 (000 T028, T039): nhật ký ghi trong transaction của lệnh, cùng commit hoặc cùng rollback; gọi
 * ngoài transaction thì lỗi; thời điểm lấy từ đồng hồ máy chủ.
 */
@Import(AuditLogAtomicityIT.Users.class)
class AuditLogAtomicityIT extends MySqlIntegrationTestBase {

    private static final AuditAction ACTION = new AuditAction("SAMPLE_CHANGE", true);

    @TestConfiguration
    static class Users {
        @Bean
        CurrentUser currentUser() {
            return FakePermissions.user(7, Role.NURSING_HOME_MANAGER);
        }
    }

    @Autowired
    AuditLogApi auditLog;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TestClockControl clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private AuditLogEntry entry(String entityId) {
        return AuditLogEntry.builder()
                .action(ACTION)
                .entity("M15.SAMPLE", entityId)
                .resident(42L)
                .before(Map.of("note", "cũ"))
                .after(Map.of("note", "mới"))
                .reason(new Reason("sửa sai"))
                .build();
    }

    private int count(String entityId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_log WHERE entity_id = ?", Integer.class, entityId);
    }

    @Test
    void FR_047_write_commitsWithCommand() {
        String id = UUID.randomUUID().toString();

        tx.executeWithoutResult(s -> auditLog.write(entry(id)));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT actor_type, actor_id, role, action, resident_id, reason,"
                        + " JSON_UNQUOTE(JSON_EXTRACT(data_after, '$.note')) AS after_note"
                        + " FROM audit_log WHERE entity_id = ?",
                id);
        assertThat(row)
                .containsEntry("actor_type", "USER")
                .containsEntry("actor_id", 7L)
                .containsEntry("role", "AC-01")
                .containsEntry("action", "SAMPLE_CHANGE")
                .containsEntry("resident_id", 42L)
                .containsEntry("reason", "sửa sai")
                .containsEntry("after_note", "mới");
    }

    @Test
    void FR_047_commandFailsAfterWrite_bothRollBack() {
        String id = UUID.randomUUID().toString();

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
                    auditLog.write(entry(id));
                    throw new IllegalStateException("lệnh lỗi sau khi ghi nhật ký");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(count(id)).isZero();
    }

    @Test
    void FR_047_writeOutsideTransaction_isRejected() {
        assertThatThrownBy(() -> auditLog.write(entry(UUID.randomUUID().toString())))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void FR_037b_writeOutsideTransaction_survivesRollbackOfCaller() {
        String id = UUID.randomUUID().toString();

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
                    auditLog.writeOutsideTransaction(entry(id));
                    throw new IllegalStateException("áp dụng lỗi");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(count(id)).isEqualTo(1);
    }

    @Test
    void FR_046_occurredAt_comesFromServerClock() {
        String id = UUID.randomUUID().toString();
        clock.setTo(Instant.parse("2027-03-01T01:02:03Z"));

        tx.executeWithoutResult(s -> auditLog.write(entry(id)));

        String occurredAt = jdbc.queryForObject(
                "SELECT DATE_FORMAT(occurred_at, '%Y-%m-%d %H:%i') FROM audit_log WHERE entity_id = ?",
                String.class, id);
        assertThat(occurredAt).isEqualTo("2027-03-01 01:02");
    }

    @Test
    void FR_046_deviceTime_isStoredForOfflineRecords() {
        String id = UUID.randomUUID().toString();
        AuditLogEntry offline = AuditLogEntry.builder()
                .action(new AuditAction("SAMPLE_RECORDED", false))
                .entity("M15.SAMPLE", id)
                .deviceTime(Instant.parse("2026-10-19T01:10:00Z"))
                .build();

        tx.executeWithoutResult(s -> auditLog.write(offline));

        assertThat(jdbc.queryForObject(
                        "SELECT DATE_FORMAT(device_time, '%H:%i') FROM audit_log WHERE entity_id = ?",
                        String.class, id))
                .isEqualTo("01:10");
    }

    @Test
    void FR_045_basis_isStoredAsJson() {
        String id = UUID.randomUUID().toString();
        AuditLogEntry entry = AuditLogEntry.builder()
                .action(new AuditAction("SAMPLE_APPLIED", false))
                .entity("M15.SAMPLE", id)
                .basis(new Basis("BR-M15-10", 5L, null, "4f1c2d3e-0000-4000-8000-000000000009"))
                .build();

        tx.executeWithoutResult(s -> auditLog.write(entry));

        assertThat(jdbc.queryForObject(
                        "SELECT JSON_EXTRACT(basis, '$.approvalRequestId') FROM audit_log WHERE entity_id = ?",
                        String.class,
                        id))
                .isEqualTo("5");
    }
}
