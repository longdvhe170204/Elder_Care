package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.incident.internal.alert.domain.ProgressionGroup;
import org.junit.jupiter.api.Test;

/**
 * Chuyển theo dõi cần diễn biến đánh giá và xử lý; đóng theo mức (BR-M05-09, bảng FR-045; T032) qua HTTP với quyền
 * thật: Trưởng tầng, Điều dưỡng có quyền "theo phân công".
 */
class BR_M05_09_CloseIncidentIT extends IncidentHttpTestBase {
    private static final String CLOSE = "/api/v1/incidents/{id}/close";
    private static final String MONITOR = "/api/v1/incidents/{id}/move-to-monitoring";

    @Test
    void FR_045_moveToMonitoringNeedsEvaluationAndActionProgressions() throws Exception {
        long id = incident("FALL", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        post(nurse, MONITOR, id, "{}").andExpect(status().isConflict());
        progression(id, ProgressionGroup.EVALUATION);
        post(nurse, MONITOR, id, "{}").andExpect(status().isConflict());
        progression(id, ProgressionGroup.ACTION);
        post(nurse, MONITOR, id, "{}").andExpect(status().isOk());

        assertThat(incidentStatus(id)).isEqualTo("MONITORING");
    }

    @Test
    void BR_M05_09_floorManagerClosesLowIncident() throws Exception {
        long id = incident("FALL", "LOW", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        post(floorManager, CLOSE, id, "{\"resolution\":\"Đã ổn\"}").andExpect(status().isOk());

        var row = jdbc.queryForMap("SELECT status, resolution, closed_by FROM incident WHERE id = ?", id);
        assertThat(row.get("status")).isEqualTo("CLOSED");
        assertThat(row.get("resolution")).isEqualTo("Đã ổn");
        assertThat(((Number) row.get("closed_by")).longValue()).isEqualTo(floorManager.accountId());
    }

    @Test
    void BR_M05_09_mediumIncidentOnlyNurseOrDoctorClosesWithResolution() throws Exception {
        long id = incident("FALL", "MEDIUM", "MONITORING", nurse.accountId(), nurse.accountId());

        post(floorManager, CLOSE, id, "{\"resolution\":\"Đã ổn\"}").andExpect(status().isForbidden());
        post(nurse, CLOSE, id, "{\"resolution\":\" \"}").andExpect(status().isUnprocessableContent());
        assertThat(incidentStatus(id)).isEqualTo("MONITORING");

        post(nurse, CLOSE, id, "{\"resolution\":\"Đã ổn định\"}").andExpect(status().isOk());
        assertThat(incidentStatus(id)).isEqualTo("CLOSED");
    }

    @Test
    void FR_045_newIncidentCannotBeClosed() throws Exception {
        long id = incident("FALL", "LOW", "NEW", null, nurse.accountId());
        post(nurse, CLOSE, id, "{\"resolution\":\"Đã ổn\"}").andExpect(status().isConflict());
        assertThat(incidentStatus(id)).isEqualTo("NEW");
    }

    private void progression(long incident, ProgressionGroup group) throws Exception {
        post(
                        nurse,
                        "/api/v1/incidents/{id}/progressions",
                        incident,
                        "{\"progressionTypeId\":" + progressionType(group) + ",\"content\":\"Ghi diễn biến\"}")
                .andExpect(status().isCreated());
    }
}
