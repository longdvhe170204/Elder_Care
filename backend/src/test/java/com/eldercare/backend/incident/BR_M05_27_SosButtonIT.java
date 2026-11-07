package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * BR-M05-27, US12 #1, #3, SC-017 (T067): lần bấm SOS tạo cảnh báo Khẩn cấp khóa {@code SOS} có phòng, giường lúc bấm;
 * lần bấm sau gộp; cùng {@code Idempotency-Key} không tăng số lần; người nhận như FR-047a, không có người liên hệ chính,
 * Quản lý viện.
 */
class BR_M05_27_SosButtonIT extends SosTestBase {

    @Test
    void BR_M05_27_pressCreatesCriticalSosAlertWithRoomAndBed() throws Exception {
        String body = press("k1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.contacts.emergencyNumber").value("115"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long alertId = ((Number) JsonPath.read(body, "$.alertId")).longValue();
        long pressId = ((Number) JsonPath.read(body, "$.pressId")).longValue();
        var alert = jdbc.queryForMap(
                "SELECT type, level, floor_area_id, occurrence_count, created_by_id FROM alert WHERE id = ?", alertId);
        assertThat(alert.get("type")).isEqualTo("RESIDENT_HELP_REQUEST");
        assertThat(alert.get("level")).isEqualTo("CRITICAL");
        assertThat(((Number) alert.get("floor_area_id")).longValue()).isEqualTo(floor);
        assertThat(((Number) alert.get("occurrence_count")).intValue()).isEqualTo(1);
        assertThat(alert.get("created_by_id")).isNull();
        var press = jdbc.queryForMap(
                "SELECT JSON_VALUE(content, '$.roomId' RETURNING SIGNED) room, JSON_VALUE(content, '$.bedId' RETURNING"
                        + " SIGNED) bed, JSON_VALUE(content, '$.residentStatus') st FROM record_event WHERE id = ?",
                pressId);
        assertThat(((Number) press.get("room")).longValue()).isEqualTo(ROOM);
        assertThat(((Number) press.get("bed")).longValue()).isEqualTo(BED);
        assertThat(press.get("st")).isEqualTo("IN_RESIDENCE");

        mvc.perform(as(nurse, MockMvcRequestBuilders.get("/api/v1/alerts/{id}", alertId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origin").value("SOS"));
    }

    @Test
    void BR_M05_27_secondAndThirdPressMergeIntoOpenAlert() throws Exception {
        press("k1").andExpect(status().isCreated());
        press("k2").andExpect(status().isCreated());
        press("k3")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertId").value(sosAlert()));

        assertThat(sosAlerts()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT occurrence_count FROM alert WHERE id = ?", Integer.class, sosAlert()))
                .isEqualTo(3);
        assertThat(presses()).isEqualTo(3);
        assertThat(events.stream(com.eldercare.backend.incident.api.events.AlertCreated.class))
                .filteredOn(event -> "SOS".equals(event.eventKey()))
                .hasSize(1);
    }

    @Test
    void SC_017_sameIdempotencyKeyDoesNotCountTwice() throws Exception {
        String first = press("same")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        press("same")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pressId").value(((Number) JsonPath.read(first, "$.pressId")).longValue()));

        assertThat(presses()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT occurrence_count FROM alert WHERE id = ?", Integer.class, sosAlert()))
                .isEqualTo(1);
    }

    @Test
    void FR_047e_recipientsAsFr047aWithoutPrimaryContactOrManager() throws Exception {
        press("k1").andExpect(status().isCreated());

        assertThat(createdGroups())
                .contains(
                        new RecipientGroup.AssignedNurse(resident, new Substitution.Default()),
                        new RecipientGroup.ShiftLead(floor, new Substitution.Default()),
                        new RecipientGroup.FloorManager(floor, new Substitution.Default()),
                        new RecipientGroup.OnDutyDoctor(new Substitution.Default()))
                .doesNotContain(
                        new RecipientGroup.PrimaryContact(resident),
                        new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER))
                .noneMatch(group -> group instanceof RecipientGroup.TripLeader);
        assertThat(allRequests())
                .flatMap(request -> request.recipientGroups())
                .noneMatch(group -> group instanceof RecipientGroup.PrimaryContact
                        || group instanceof RecipientGroup.Representative);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM incident WHERE resident_id = ?", Integer.class, resident))
                .isZero();
    }

    @Test
    void FR_047b_noOnDutyDoctorNotifiesAllDoctorsAndManager() throws Exception {
        when(staff.onDutyDoctors(any())).thenReturn(List.of());

        press("k1").andExpect(status().isCreated());

        assertThat(createdGroups())
                .contains(
                        new RecipientGroup.FacilityRole(Role.DOCTOR),
                        new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER))
                .noneMatch(group -> group instanceof RecipientGroup.OnDutyDoctor);
        assertThat(jdbc.queryForObject("SELECT no_on_duty_doctor FROM alert WHERE id = ?", Boolean.class, sosAlert()))
                .isTrue();
    }
}
