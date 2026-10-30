package com.eldercare.backend.administration.auditlog.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.ReasonRequiredException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** FR-044, FR-043a (000 T027): build() báo lỗi khi hành động bắt buộc lý do mà thiếu lý do. */
class AuditLogEntryTest {

    private static final AuditAction CHANGE = new AuditAction("PARAMETER_CHANGE", true);
    private static final AuditAction LOGIN = new AuditAction("LOGIN_SUCCEEDED", false);

    @Test
    void FR_044_actionRequiringReason_withoutReason_isRejected() {
        assertThatThrownBy(() -> AuditLogEntry.builder()
                        .action(CHANGE)
                        .entity("M15.PARAMETER", "CFG-M15-05")
                        .build())
                .isInstanceOf(ReasonRequiredException.class);
    }

    @Test
    void FR_044_actionRequiringReason_withReason_builds() {
        AuditLogEntry entry = AuditLogEntry.builder()
                .action(CHANGE)
                .entity("M15.PARAMETER", "CFG-M15-05")
                .before(Map.of("value", 48))
                .after(Map.of("value", 24))
                .reason(new Reason("Rút ngắn thời hạn nhắc"))
                .build();

        assertThat(entry.action()).isEqualTo(CHANGE);
        assertThat(entry.entityId()).isEqualTo("CFG-M15-05");
        assertThat(entry.reason()).isEqualTo("Rút ngắn thời hạn nhắc");
    }

    @Test
    void FR_043a_actionWithoutDataChange_buildsWithoutBeforeAfterOrReason() {
        AuditLogEntry entry =
                AuditLogEntry.builder().action(LOGIN).entity("M15.ACCOUNT", 7L).build();

        assertThat(entry.before()).isNull();
        assertThat(entry.after()).isNull();
        assertThat(entry.reason()).isNull();
        assertThat(entry.entityId()).isEqualTo("7");
    }

    @Test
    void entityAndAction_areRequired() {
        assertThatThrownBy(() -> AuditLogEntry.builder().entity("X", 1).build())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> AuditLogEntry.builder().action(LOGIN).build())
                .isInstanceOf(IllegalStateException.class);
    }
}
