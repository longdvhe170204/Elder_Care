package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.api.events.EmergencyIncidentActivated;
import java.util.List;
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
class BR_42_EmergencyNotificationIT extends EmergencyTestBase {

    @Autowired
    ApplicationEvents events;

    @Test
    void BR_42_raisingToCriticalActivatesEmergency() throws Exception {
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

    private static List<RecipientGroup> groups(List<NotificationRequest> requests) {
        return requests.stream().flatMap(r -> r.recipientGroups().stream()).toList();
    }
}
