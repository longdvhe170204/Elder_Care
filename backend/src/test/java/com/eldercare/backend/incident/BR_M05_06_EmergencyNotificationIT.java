package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.api.events.EmergencyIncidentActivated;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * BR-M05-06, FR-047 (a), FR-047c (US5 #2; K1): sự cố được nâng lên Khẩn cấp hoặc kích hoạt từ cảnh báo chạy quy trình
 * khẩn cấp trong cùng lệnh: một sự kiện kích hoạt mang yêu cầu song song tới Bác sĩ trực, Trưởng tầng, Quản lý viện,
 * Điều dưỡng phụ trách, Người phụ trách ca và người liên hệ chính (nội dung tối thiểu, mã Q-354); lưu bản chụp thẻ.
 */
@RecordApplicationEvents
class BR_M05_06_EmergencyNotificationIT extends EmergencyTestBase {

    @Autowired
    ApplicationEvents events;

    @Test
    void BR_M05_06_raisingToCriticalActivatesEmergency() throws Exception {
        long id = incident("FALL", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        post(nurse, "/api/v1/incidents/{id}/change-level", id, "{\"newLevel\":\"CRITICAL\",\"reason\":\"Ngừng thở\"}")
                .andExpect(status().isOk());

        var activated = events.stream(EmergencyIncidentActivated.class).toList();
        assertThat(activated).singleElement().satisfies(event -> {
            assertThat(event.incidentId()).isEqualTo(id);
            assertThat(event.noOnDutyDoctor()).isFalse();
            assertThat(groups(event.notificationRequests()))
                    .hasAtLeastOneElementOfType(RecipientGroup.OnDutyDoctor.class)
                    .hasAtLeastOneElementOfType(RecipientGroup.FloorManager.class)
                    .hasAtLeastOneElementOfType(RecipientGroup.AssignedNurse.class)
                    .hasAtLeastOneElementOfType(RecipientGroup.ShiftLead.class)
                    .contains(
                            new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER),
                            new RecipientGroup.PrimaryContact(resident));
        });
        assertThat(count("M05.EMERGENCY_CARD", id)).isEqualTo(1);
    }

    @Test
    void FR_047c_primaryContactRequestCarriesCaseCodeAndMinimalContent() throws Exception {
        long id = incident("FALL", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        post(nurse, "/api/v1/incidents/{id}/change-level", id, "{\"newLevel\":\"CRITICAL\",\"reason\":\"Ngừng thở\"}")
                .andExpect(status().isOk());

        var family = events.stream(EmergencyIncidentActivated.class)
                .flatMap(event -> event.notificationRequests().stream())
                .filter(r -> r.recipientGroups().contains(new RecipientGroup.PrimaryContact(resident)))
                .toList();
        assertThat(family).singleElement().satisfies(request -> {
            assertThat(request.recipientGroups()).containsExactly(new RecipientGroup.PrimaryContact(resident));
            assertThat(request.informationSubject()).isEqualTo(Optional.of(InformationSubject.ABOUT_RESIDENT));
            assertThat(request.q354Case()).isEqualTo(Optional.of(Q354Case.PRIMARY_CONTACT_EMERGENCY));
            assertThat(request.content())
                    .allSatisfy(part ->
                            assertThat(part.content()).doesNotContain("Penicillin", "Amlodipin", "nguyện vọng"));
        });
    }

    @Test
    void FR_029_activateEmergencyFromAlert_convertsAlertAndRunsEmergency() throws Exception {
        long alertId = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) + 1000 FROM alert", Long.class);
        jdbc.update(
                """
                INSERT INTO alert (id, resident_id, floor_area_id, type, type_lock, level, first_at, latest_at, status,
                  version)
                VALUES (?, ?, ?, 'CHI_SO', 'VUOT_NGUONG:1', 'CRITICAL', ?, ?, 'NEW', 0)
                """,
                alertId,
                resident,
                floor,
                java.sql.Timestamp.from(time.now()),
                java.sql.Timestamp.from(time.now()));

        post(
                        nurse,
                        "/api/v1/alerts/{id}/activate-emergency",
                        alertId,
                        "{\"incidentTypeId\":" + typeId("FALL") + ",\"description\":\"SpO2 tụt, ngừng thở\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incident.level").value("CRITICAL"))
                .andExpect(jsonPath("$.emergencyCard.allergies[0].name").value("Penicillin"));

        var alert = jdbc.queryForMap("SELECT status, incident_id FROM alert WHERE id = ?", alertId);
        assertThat(alert.get("status")).isEqualTo("CONVERTED_TO_INCIDENT");
        long incident = ((Number) alert.get("incident_id")).longValue();
        assertThat(jdbc.queryForObject("SELECT level FROM incident WHERE id = ?", String.class, incident))
                .isEqualTo("CRITICAL");
        assertThat(count("M05.EMERGENCY_CARD", incident)).isEqualTo(1);
        assertThat(events.stream(EmergencyIncidentActivated.class)).singleElement();
    }

    private static List<RecipientGroup> groups(List<NotificationRequest> requests) {
        return requests.stream().flatMap(r -> r.recipientGroups().stream()).toList();
    }
}
