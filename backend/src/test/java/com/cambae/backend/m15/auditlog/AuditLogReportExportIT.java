package com.cambae.backend.m15.auditlog;

import static com.cambae.backend.m15.auditlog.AuditLogFixtures.MANAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.ReasonRequiredException;
import com.cambae.backend.m15.sample.SampleReportService;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * US3 #11; FR-043a, FR-044 (000 T065): lần xuất báo cáo có định danh ghi đúng một bản nhật ký {@code REPORT_EXPORTED},
 * trước/sau rỗng, lý do là mục đích xuất, bộ lọc trong chi tiết; thiếu mục đích thì không ghi; xem nhật ký trên hệ
 * thống không ghi nhật ký (FR-049).
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(AuditLogFixtures.class)
class AuditLogReportExportIT extends MySqlIntegrationTestBase {

    @Autowired
    SampleReportService reports;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    private int countFor(String reportCode) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = ? AND entity_id = ?",
                Integer.class,
                SampleReportService.REPORT,
                reportCode);
    }

    @Test
    void US3_11_export_writesOneEntry_emptyBeforeAfter_purposeAsReason_filterInDetails() {
        String report = "R-" + UUID.randomUUID();
        user.set(MANAGER);

        reports.export(report, new Reason("họp giao ban"), Map.of("floor", 2, "month", "2026-10"));

        assertThat(countFor(report)).isEqualTo(1);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT action, reason, data_before, data_after,"
                        + " JSON_EXTRACT(details, '$.filter.floor') AS floor FROM audit_log"
                        + " WHERE entity_type = ? AND entity_id = ?",
                SampleReportService.REPORT,
                report);
        assertThat(row)
                .containsEntry("action", "REPORT_EXPORTED")
                .containsEntry("reason", "họp giao ban")
                .containsEntry("data_before", null)
                .containsEntry("data_after", null);
        assertThat(String.valueOf(row.get("floor"))).isEqualTo("2");
    }

    @Test
    void FR_044_exportWithoutPurpose_isRejected_andNothingLogged() {
        String report = "R-" + UUID.randomUUID();
        user.set(MANAGER);

        assertThatThrownBy(() -> reports.export(report, null, Map.of())).isInstanceOf(ReasonRequiredException.class);
        assertThat(countFor(report)).isZero();
    }

    @Test
    void FR_049_viewingAuditLog_isNotLogged() throws Exception {
        user.set(MANAGER);
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class);

        mvc.perform(get("/api/v1/audit-logs")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/audit-logs/entities/" + AuditLogFixtures.PROFILE + "/x"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_log", Integer.class))
                .isEqualTo(before);
    }
}
