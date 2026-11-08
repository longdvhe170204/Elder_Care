package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Hủy ghi nhận sự cố qua đính chính của 000 (BR-M05-21, FR-046, FR-046a; T033, T036): sự cố từng Khẩn cấp chỉ Điều
 * dưỡng, Bác sĩ; sự cố lây nhiễm còn vùng khoanh chỉ gắn nó bị từ chối; tác động cũ không thu hồi mà được báo lại.
 */
@RecordApplicationEvents
class BR_M05_21_CancelEmergencyIncidentIT extends IncidentHttpTestBase {
    @Autowired
    ContainmentStore containments;

    @Autowired
    ApplicationEvents events;

    @Autowired
    com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore fallMonitoring;

    @Test
    void BR_M05_21_floorManagerCannotVoidEverEmergencyIncident() throws Exception {
        long id = incident("FALL", "CRITICAL", "CLOSED", nurse.accountId(), floorManager.accountId());

        voidIncident(floorManager, id).andExpect(status().isForbidden());

        assertThat(incidentStatus(id)).isEqualTo("CLOSED");
    }

    private org.springframework.test.web.servlet.ResultActions voidIncident(
            com.eldercare.backend.support.TestUsers.TestUser as, long id) throws Exception {
        return post(as, "/api/v1/corrections/M05.INCIDENT/{id}", id, "{\"type\":\"VOID\",\"reason\":\"Ghi nhầm\"}");
    }
}
