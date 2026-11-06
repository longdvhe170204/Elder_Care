package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.common.Role;
import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * BR-M05-31, FR-047k, FR-048b, SC-020 (T098; quickstart H7, H8): thông tin đọc cho tổng đài theo quyền xem sức khỏe;
 * lượt gọi, kết quả một lần, chỉ người bấm; Đã gọi được thêm diễn biến {@code CALL_EMERGENCY}; lượt trên cảnh báo được
 * chép khi chuyển sự cố.
 */
class BR_M05_31_Call115IT extends AssistTestBase {

    @Test
    void H7_infoForOperatorWithHealthFieldsByPermission() throws Exception {
        jdbc.update(
                "UPDATE parameter SET current_value = CAST(? AS JSON) WHERE code = 'CFG-M11-11'",
                "{\"name\":\"Viện An Bình\",\"address\":\"12 Lê Lợi\",\"phone\":\"0281234567\"}");
        long incident = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        mvc.perform(as(nurse, MockMvcRequestBuilders.get("/api/v1/incidents/{id}/call-115-info", incident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facility.address").value("12 Lê Lợi"))
                .andExpect(jsonPath("$.floor").value("Tầng"))
                .andExpect(jsonPath("$.roomOrPlace").value("P11"))
                .andExpect(jsonPath("$.resident.fullName").value("Nguyễn Thị An"))
                .andExpect(jsonPath("$.allergies[0]").value("Penicillin"))
                .andExpect(jsonPath("$.hiddenFields").isEmpty());

        doReturn(Visibility.PRESENCE_ONLY).when(visibility).canView(any(), any(), any());
        mvc.perform(as(nurse, MockMvcRequestBuilders.get("/api/v1/incidents/{id}/call-115-info", incident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allergies").doesNotExist())
                .andExpect(jsonPath("$.hiddenFields[0]").value("allergies"));
    }

    @Test
    void H7_connectedCallAddsProgressionOnce() throws Exception {
        long incident = incident("FALL", "CRITICAL", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        String body = post(nurse, "/api/v1/incidents/{id}/call-115", incident, "{}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long attempt = ((Number) JsonPath.read(body, "$.id")).longValue();

        var doctor = users.staff(Role.DOCTOR);
        post(doctor, "/api/v1/call-115-attempts/{id}/result", attempt, "{\"result\":\"CONNECTED\"}")
                .andExpect(status().isForbidden());
        post(nurse, "/api/v1/call-115-attempts/{id}/result", attempt, "{\"result\":\"CONNECTED\"}")
                .andExpect(status().isOk());
        post(nurse, "/api/v1/call-115-attempts/{id}/result", attempt, "{\"result\":\"NOT_CONNECTED\"}")
                .andExpect(status().isConflict());

        assertThat(callProgressions(incident)).isEqualTo(1);
        var pressedAt =
                jdbc.queryForObject("SELECT occurred_at FROM record_event WHERE id = ?", Timestamp.class, attempt);
        assertThat(jdbc.queryForObject(
                        "SELECT occurred_at FROM record_event WHERE kind = 'M05.INCIDENT_PROGRESSION'"
                                + " AND entity_id = ? AND JSON_VALUE(content, '$.typeCode') = 'CALL_EMERGENCY'",
                        Timestamp.class,
                        incident))
                .isEqualTo(pressedAt);
    }

    @Test
    void H8_callOnAlertCopiedWhenActivatingEmergency() throws Exception {
        press("k1").andExpect(status().isCreated());
        long alertId = sosAlert();
        String body = post(nurse, "/api/v1/alerts/{id}/call-115", alertId, "{}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long attempt = ((Number) JsonPath.read(body, "$.id")).longValue();
        post(nurse, "/api/v1/call-115-attempts/{id}/result", attempt, "{\"result\":\"CONNECTED\"}")
                .andExpect(status().isOk());

        post(
                        nurse,
                        "/api/v1/alerts/{id}/activate-emergency",
                        alertId,
                        "{\"incidentTypeId\":" + typeId("FALL") + ",\"description\":\"Ngã\"}")
                .andExpect(status().isCreated());

        long incident = jdbc.queryForObject("SELECT incident_id FROM alert WHERE id = ?", Long.class, alertId);
        assertThat(callProgressions(incident)).isEqualTo(1);
    }

    @Test
    void FR_047k_onlyCriticalAlerts() throws Exception {
        long incident = incident("FALL", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        post(nurse, "/api/v1/incidents/{id}/call-115", incident, "{}").andExpect(status().isConflict());
    }

    private int callProgressions(long incident) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM record_event WHERE kind = 'M05.INCIDENT_PROGRESSION' AND entity_id = ?"
                        + " AND JSON_VALUE(content, '$.typeCode') = 'CALL_EMERGENCY'",
                Integer.class,
                incident);
    }
}
