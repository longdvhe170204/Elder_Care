package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * BR-M05-29, US13 #1, #2, #4 → #6, SC-019 (T097; quickstart H1, H2, H4 → H6): trong ca mới bấm được, ngoài ca 409
 * {@code NOT_ON_SHIFT} kèm số liên hệ; gộp theo người bấm; chi tiết đặt một lần, người cao tuổi gắn vào cảnh báo; chuyển
 * sự cố, kích hoạt khẩn cấp thiếu người cao tuổi → 422 {@code RESIDENT_REQUIRED}; quyền theo FR-080.
 */
class BR_M05_29_EmergencyAssistButtonIT extends AssistTestBase {

    @Test
    void H1_pressCreatesCriticalAssistAlertWithoutResident() throws Exception {
        pressAssist(presser)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"));

        var alert = jdbc.queryForMap(
                "SELECT resident_id, floor_area_id, level, type, occurrence_count FROM alert WHERE id = ?",
                assistAlert(presser));
        assertThat(alert.get("resident_id")).isNull();
        assertThat(((Number) alert.get("floor_area_id")).longValue()).isEqualTo(floor);
        assertThat(alert.get("level")).isEqualTo("CRITICAL");
        mvc.perform(as(floorManager, MockMvcRequestBuilders.get("/api/v1/alerts/{id}", assistAlert(presser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("ASSIST_BUTTON"));
    }

    @Test
    void H4_offShiftRefusedAndRepeatedPressMerges() throws Exception {
        var outside = users.staff(Role.NURSING_STAFF);
        pressAssist(outside)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_ON_SHIFT"))
                .andExpect(jsonPath("$.contacts.emergencyNumber").value("115"));

        pressAssist(presser).andExpect(status().isCreated());
        pressAssist(presser).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM alert WHERE type = 'STAFF_ASSIST_REQUEST' AND requester_id = ?",
                        Integer.class,
                        presser.accountId()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT occurrence_count FROM alert WHERE id = ?", Integer.class, assistAlert(presser)))
                .isEqualTo(2);
    }

    @Test
    void H2_detailsSetOnceAndAttachResident() throws Exception {
        pressAssist(presser).andExpect(status().isCreated());
        long alertId = assistAlert(presser);

        details(presser, alertId, "{\"eventType\":\"FALL\",\"place\":\"RESTROOM\",\"residentId\":" + resident + "}")
                .andExpect(status().isNoContent());
        details(presser, alertId, "{\"residentId\":" + TestUsers.nextId() + "}").andExpect(status().isConflict());
        details(nurse, alertId, "{\"eventType\":\"OTHER\"}").andExpect(status().isNotFound());

        doReturn(Optional.of("Nguyễn Văn Bấm")).when(accounts).fullName(presser.accountId());
        mvc.perform(as(floorManager, MockMvcRequestBuilders.get("/api/v1/alerts/{id}", alertId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pressedBy").value("Nguyễn Văn Bấm"))
                .andExpect(jsonPath("$.eventType").value("FALL"))
                .andExpect(jsonPath("$.chosenPlace").value("RESTROOM"))
                .andExpect(jsonPath("$.allowedCommands").value(org.hamcrest.Matchers.hasItem("call-115")));
        assertThat(jdbc.queryForObject("SELECT resident_id FROM alert WHERE id = ?", Long.class, alertId))
                .isEqualTo(resident);
        assertThat(detailGroups())
                .contains(new RecipientGroup.AssignedNurse(
                        resident, new com.eldercare.backend.common.notification.Substitution.Default()));
    }

    @Test
    void H5_receiverMarksArrivedAndPresserSeesArrived() throws Exception {
        doReturn(Optional.of("Trưởng Tầng Lan")).when(accounts).fullName(floorManager.accountId());
        pressAssist(presser).andExpect(status().isCreated());
        long alertId = assistAlert(presser);
        post(floorManager, "/api/v1/alerts/{id}/receive", alertId, "{}").andExpect(status().isOk());

        post(floorManager, "/api/v1/alerts/{id}/mark-arrived", alertId, "{}").andExpect(status().isOk());
        post(floorManager, "/api/v1/alerts/{id}/mark-arrived", alertId, "{}").andExpect(status().isConflict());

        mvc.perform(as(presser, MockMvcRequestBuilders.get(ASSIST + "/current")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARRIVED"))
                .andExpect(jsonPath("$.receiverName").value("Trưởng Tầng Lan"));
    }

    @Test
    void H6_activateWithoutResidentRequiresResident() throws Exception {
        pressAssist(presser).andExpect(status().isCreated());
        long alertId = assistAlert(presser);
        String body = "{\"incidentTypeId\":" + typeId("FALL") + ",\"description\":\"Ngã cầu thang\"";

        post(floorManager, "/api/v1/alerts/{id}/activate-emergency", alertId, body + "}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("RESIDENT_REQUIRED"));
        post(
                        floorManager,
                        "/api/v1/alerts/{id}/activate-emergency",
                        alertId,
                        body + ",\"residentId\":" + resident + "}")
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT resident_id FROM alert WHERE id = ?", Long.class, alertId))
                .isEqualTo(resident);
        assertThat(allRequests())
                .flatMap(request -> request.recipientGroups())
                .contains(new RecipientGroup.PrimaryContact(resident));
    }

    @Test
    void FR_029_convertWithoutResidentRequiresResident() throws Exception {
        pressAssist(presser).andExpect(status().isCreated());
        long alertId = assistAlert(presser);
        post(floorManager, "/api/v1/alerts/{id}/receive", alertId, "{}").andExpect(status().isOk());
        post(floorManager, "/api/v1/alerts/{id}/start-handling", alertId, "{}").andExpect(status().isOk());

        post(
                        floorManager,
                        "/api/v1/alerts/{id}/convert-to-incident",
                        alertId,
                        "{\"incidentTypeId\":" + typeId("FALL") + "}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("RESIDENT_REQUIRED"));
    }

    @Test
    void FR_080_accountantCanPressFamilyCannot() throws Exception {
        var accountant = users.staff(Role.ACCOUNTANT);
        when(staff.onShift(eq(accountant.staffId()), any())).thenReturn(true);
        pressAssist(accountant).andExpect(status().isCreated());

        var family = users.family(TestUsers.nextId());
        pressAssist(family).andExpect(status().isForbidden());
    }
}
