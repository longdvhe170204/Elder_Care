package com.eldercare.backend.administration.auditlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.Basis;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** FR-045 (000 T027, T028): thay đổi do Bộ lập lịch ghi người thực hiện SCHEDULER và bắt buộc có căn cứ. */
@Import(AuditLogSchedulerIT.Scheduler.class)
class AuditLogSchedulerIT extends MySqlIntegrationTestBase {

    private static final AuditAction APPLY = new AuditAction("SAMPLE_APPLIED", false);

    @TestConfiguration
    static class Scheduler {
        @Bean
        CurrentUser currentUser() {
            return FakePermissions.user(0, Role.SCHEDULER);
        }
    }

    @Autowired
    AuditLogApi auditLog;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void FR_045_schedulerWithoutBasis_isRejected() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .action(APPLY)
                .entity("M15.SAMPLE", "s-1")
                .build();

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> auditLog.write(entry)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void FR_045_schedulerWithBasis_recordsSchedulerAsActor() {
        String id = UUID.randomUUID().toString();
        AuditLogEntry entry = AuditLogEntry.builder()
                .action(APPLY)
                .entity("M15.SAMPLE", id)
                .basis(new Basis(null, 5L, null, "4f1c2d3e-0000-4000-8000-000000000009"))
                .build();

        tx.executeWithoutResult(s -> auditLog.write(entry));

        assertThat(jdbc.queryForMap("SELECT actor_type, actor_id, role FROM audit_log WHERE entity_id = ?", id))
                .containsEntry("actor_type", "SCHEDULER")
                .containsEntry("actor_id", null)
                .containsEntry("role", "AC-11");
    }
}
