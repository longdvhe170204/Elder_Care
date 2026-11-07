package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.incident.api.events.CallReminderStopRequested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Hạ sự cố khỏi Khẩn cấp: chỉ Bác sĩ, phát yêu cầu dừng nhắc gọi cho 009, giữ dấu từng Khẩn cấp; mỗi lần đổi mức có một
 * dòng lịch sử (BR-M05-22, FR-044; data-model {@code doi_muc_su_co}; T033, T034).
 */
@RecordApplicationEvents
class BR_M05_22_LowerLevelStopsCallsIT extends IncidentHttpTestBase {
    private static final String CHANGE = "/api/v1/incidents/{id}/change-level";

    @Autowired
    ApplicationEvents events;

    @Test
    void BR_M05_22_onlyDoctorLowersEmergencyAndCallsStop() throws Exception {
        long id = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        post(nurse, CHANGE, id, "{\"newLevel\":\"MEDIUM\",\"reason\":\"Đã ổn\"}")
                .andExpect(status().isForbidden());
        assertThat(events.stream(CallReminderStopRequested.class)).isEmpty();

        var doctor = users.staff(Role.DOCTOR);
        post(doctor, CHANGE, id, "{\"newLevel\":\"MEDIUM\",\"reason\":\"Đã ổn định\"}")
                .andExpect(status().isOk());

        var row = jdbc.queryForMap("SELECT level, was_emergency FROM incident WHERE id = ?", id);
        assertThat(row.get("level")).isEqualTo("MEDIUM");
        assertThat(row.get("was_emergency")).isEqualTo(true);
        assertThat(events.stream(CallReminderStopRequested.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.sourceRecordType()).isEqualTo("M05.INCIDENT");
                    assertThat(event.sourceRecordId()).isEqualTo(id);
                });
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content, '$.to')) FROM record_event"
                                + " WHERE kind = 'M05.INCIDENT_LEVEL_CHANGE' AND entity_id = ?",
                        String.class,
                        id))
                .isEqualTo("MEDIUM");
    }

    @Test
    void BR_M05_22_raisingLevelDoesNotStopCalls() throws Exception {
        long id = incident("FALL", "LOW", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        post(nurse, CHANGE, id, "{\"newLevel\":\"MEDIUM\",\"reason\":\"Đau tăng\"}")
                .andExpect(status().isOk());

        assertThat(events.stream(CallReminderStopRequested.class)).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind = 'M05.INCIDENT_LEVEL_CHANGE' AND entity_id = ?",
                        Integer.class,
                        id))
                .isEqualTo(1);
    }
}
