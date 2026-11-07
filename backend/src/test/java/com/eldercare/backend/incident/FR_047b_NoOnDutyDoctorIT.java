package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.api.events.EmergencyIncidentActivated;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * FR-047b (US5 #11): kích hoạt khẩn cấp lúc không có Bác sĩ trực gửi mọi Bác sĩ đang hoạt động thay nhóm Bác sĩ trực,
 * báo Quản lý viện và gắn dấu "không có Bác sĩ trực tại thời điểm" vào sự cố.
 */
@RecordApplicationEvents
class FR_047b_NoOnDutyDoctorIT extends EmergencyTestBase {

    @Autowired
    ApplicationEvents events;

    @Test
    void FR_047b_noOnDutyDoctor_allDoctorsAndManager_andFlag() throws Exception {
        noOnDutyDoctor();
        long id = incident("FALL", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        post(nurse, "/api/v1/incidents/{id}/change-level", id, "{\"newLevel\":\"CRITICAL\",\"reason\":\"Ngừng thở\"}")
                .andExpect(status().isOk());

        assertThat(events.stream(EmergencyIncidentActivated.class))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.noOnDutyDoctor()).isTrue();
                    var groups = event.notificationRequests().stream()
                            .flatMap(r -> r.recipientGroups().stream())
                            .toList();
                    assertThat(groups)
                            .contains(
                                    new RecipientGroup.FacilityRole(Role.DOCTOR),
                                    new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER))
                            .doesNotHaveAnyElementsOfTypes(RecipientGroup.OnDutyDoctor.class);
                });
        assertThat(jdbc.queryForObject("SELECT no_on_duty_doctor FROM incident WHERE id = ?", Boolean.class, id))
                .isTrue();
    }
}
