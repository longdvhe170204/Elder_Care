package com.eldercare.backend.administration.auditlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
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
    void FR_047_commandFailsAfterWrite_bothRollBack() {
        String id = UUID.randomUUID().toString();

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
                    auditLog.write(entry(id));
                    throw new IllegalStateException("lệnh lỗi sau khi ghi nhật ký");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(count(id)).isZero();
    }
}
