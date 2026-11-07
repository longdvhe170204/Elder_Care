package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.api.events.CriticalFlagRecorded;
import com.eldercare.backend.incident.api.events.PalliativeCareChoiceRecorded;
import com.eldercare.backend.incident.api.events.TemporaryCriticalFlagConfirmationRequested;
import com.eldercare.backend.incident.api.events.TemporaryHomeLeaveRequested;
import com.eldercare.backend.incident.api.events.WishReconfirmationRequested;
import com.eldercare.backend.resident.api.dto.WishChoice;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * BR-M05-15, FR-050b, FR-050d, FR-050e (US11 #1 → #3, #5): ghi dấu tạo cảnh báo Khẩn cấp không gộp kèm thông báo đội
 * ngũ, người thân (nội dung tối thiểu, mã Q-354) và yêu cầu gọi xác nhận lại; dấu tạm của Điều dưỡng chỉ khi không có
 * Bác sĩ trực; đóng cảnh báo cần kết quả xác nhận lại và lựa chọn.
 */
@RecordApplicationEvents
class BR_M05_15_CriticalConditionIT extends CriticalCareTestBase {

    @Autowired
    ApplicationEvents events;

    @Test
    void FR_050a_flagResponseAndAlertExposeFlagId() throws Exception {
        var response = postNew(
                        doctor,
                        "/api/v1/residents/" + resident + "/critical-flags",
                        "{\"type\":\"OFFICIAL\",\"assessment\":\"Suy hô hấp nặng\"}")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var created = latestFlag();
        assertThat(response).contains("\"flagId\":" + created.id()).contains("\"alertId\":" + created.alertId());

        mvc.perform(as(doctor, MockMvcRequestBuilders.get("/api/v1/alerts/{id}", created.alertId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criticalFlagId").value(created.id()));
    }

    @Test
    void FR_050b_flagNotifiesTeamFamilyAndRequestsReconfirmation() throws Exception {
        activeWish(WishChoice.RETURN_HOME);
        long alertId = flag(doctor).alertId();

        var recorded = events.stream(CriticalFlagRecorded.class).toList();
        assertThat(recorded).singleElement().satisfies(event -> {
            assertThat(event.alertId()).isEqualTo(alertId);
            assertThat(event.type()).isEqualTo("OFFICIAL");
        });
        var requests = recorded.getFirst().notificationRequests();
        var team = request(requests, null);
        assertThat(team.recipientGroups())
                .hasAtLeastOneElementOfType(RecipientGroup.OnDutyDoctor.class)
                .hasAtLeastOneElementOfType(RecipientGroup.AssignedNurse.class)
                .hasAtLeastOneElementOfType(RecipientGroup.FloorManager.class)
                .contains(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        var primary = request(requests, Q354Case.PRIMARY_CONTACT_EMERGENCY);
        assertThat(primary.recipientGroups()).containsExactly(new RecipientGroup.PrimaryContact(resident));
        var representative = request(requests, Q354Case.WISH_RECONFIRMATION);
        assertThat(representative.recipientGroups()).containsExactly(new RecipientGroup.Representative(resident));
        for (var family : List.of(primary, representative)) {
            assertThat(family.informationSubject()).isPresent();
            assertThat(family.content())
                    .extracting(ContentPart::content)
                    .noneMatch(text -> text.contains("nguyện vọng"));
            assertThat(family.displayParams()).doesNotContainKey("endOfLifeWish");
        }

        assertThat(events.stream(WishReconfirmationRequested.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.alertId()).isEqualTo(alertId);
                    var groups = event.notificationRequests().getFirst().recipientGroups();
                    assertThat(groups)
                            .hasAtLeastOneElementOfType(RecipientGroup.OnDutyDoctor.class)
                            .hasAtLeastOneElementOfType(RecipientGroup.AssignedNurse.class);
                    assertThat(event.notificationRequests()
                                    .getFirst()
                                    .displayParams()
                                    .get("callOrder"))
                            .isEqualTo(List.of(representativeId));
                });
        assertThat(events.stream(TemporaryCriticalFlagConfirmationRequested.class))
                .isEmpty();
    }

    @Test
    void FR_050a_provisionalFlagOnlyWithoutOnDutyDoctor() throws Exception {
        post(nurse, FLAGS, resident, "{\"type\":\"PROVISIONAL\",\"assessment\":\"Thở yếu\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ON_DUTY_DOCTOR_AVAILABLE"));
        post(nurse, FLAGS, resident, "{\"type\":\"OFFICIAL\",\"assessment\":\"Thở yếu\"}")
                .andExpect(status().isForbidden());

        noOnDutyDoctor();
        post(nurse, FLAGS, resident, "{\"type\":\"PROVISIONAL\",\"assessment\":\"Thở yếu\"}")
                .andExpect(status().isCreated());
        long flagId = latestFlag().id();
        assertThat(flagStatus(flagId)).isEqualTo("PROVISIONAL");
        assertThat(events.stream(TemporaryCriticalFlagConfirmationRequested.class))
                .singleElement()
                .satisfies(event -> assertThat(
                                event.notificationRequests().getFirst().recipientGroups())
                        .containsExactly(new RecipientGroup.FacilityRole(Role.DOCTOR)));
        assertThat(events.stream(CriticalFlagRecorded.class)
                        .findFirst()
                        .orElseThrow()
                        .notificationRequests())
                .anySatisfy(
                        r -> assertThat(r.recipientGroups()).contains(new RecipientGroup.FacilityRole(Role.DOCTOR)));

        post(nurse, CONFIRM, flagId, "{}").andExpect(status().isForbidden());
        post(doctor, CONFIRM, flagId, "{}").andExpect(status().isOk());
        assertThat(flagStatus(flagId)).isEqualTo("OPEN");
        post(doctor, CONFIRM, flagId, "{}").andExpect(status().isConflict());
    }

    @Test
    void FR_050d_closeNeedsReconfirmationAndChoice() throws Exception {
        long alertId = flag(doctor).alertId();
        startHandling(alertId);
        String close = "{\"resolution\":\"Đã thực hiện nguyện vọng\"}";

        post(nurse, "/api/v1/alerts/{id}/close", alertId, close)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WISH_NOT_RECONFIRMED"));
        post(nurse, CHOICE, alertId, "{\"choice\":\"STAY_PALLIATIVE\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WISH_NOT_RECONFIRMED"));

        post(nurse, RECONFIRM, alertId, reconfirmation("KEPT")).andExpect(status().isCreated());
        post(nurse, "/api/v1/alerts/{id}/close", alertId, close)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CHOICE_NOT_RECORDED"));

        post(nurse, CHOICE, alertId, "{\"choice\":\"STAY_PALLIATIVE\"}").andExpect(status().isCreated());
        assertThat(events.stream(PalliativeCareChoiceRecorded.class))
                .singleElement()
                .satisfies(event -> assertThat(event.residentId()).isEqualTo(resident));
        post(nurse, "/api/v1/alerts/{id}/close", alertId, close).andExpect(status().isOk());
    }

    @Test
    void FR_050d_returnHomeNotifiesOfficeAndFloorManager() throws Exception {
        long alertId = flag(doctor).alertId();
        post(nurse, RECONFIRM, alertId, reconfirmation("KEPT")).andExpect(status().isCreated());

        post(nurse, CHOICE, alertId, "{\"choice\":\"RETURN_HOME\"}").andExpect(status().isCreated());

        assertThat(events.stream(TemporaryHomeLeaveRequested.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.alertId()).isEqualTo(alertId);
                    assertThat(event.notificationRequests().getFirst().recipientGroups())
                            .contains(new RecipientGroup.FacilityRole(Role.OFFICE_STAFF))
                            .hasAtLeastOneElementOfType(RecipientGroup.FloorManager.class);
                });
        assertThat(count("M05.CHOICE_EXECUTION", alertId)).isEqualTo(1);
    }

    private static NotificationRequest request(List<NotificationRequest> requests, Q354Case q354) {
        return requests.stream()
                .filter(r -> r.q354Case().equals(Optional.ofNullable(q354)))
                .findFirst()
                .orElseThrow();
    }
}
