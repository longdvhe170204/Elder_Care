package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.incident.api.events.EffectiveImpactsRemain;
import com.eldercare.backend.incident.internal.incident.service.IncidentCorrectionEffects;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import java.util.Set;
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

    @Test
    void BR_M05_21_nurseRecorderVoidsEverEmergencyIncident() throws Exception {
        long id = incident("FALL", "CRITICAL", "CLOSED", nurse.accountId(), nurse.accountId());

        voidIncident(nurse, id).andExpect(status().isCreated());

        assertThat(incidentStatus(id)).isEqualTo("CANCELLED");
    }

    @Test
    void BR_M05_21_floorManagerVoidsOwnNonEmergencyIncident() throws Exception {
        long id = incident("FALL", "LOW", "IN_PROGRESS", nurse.accountId(), floorManager.accountId());

        voidIncident(floorManager, id).andExpect(status().isCreated());

        assertThat(incidentStatus(id)).isEqualTo("CANCELLED");
    }

    @Test
    void FR_066_infectionWithIsolationOnlyForItCannotBeVoided() throws Exception {
        long id = incident("SUSPECTED_INFECTION", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        var isolation = containments.create(
                new ContainmentScope(Set.of(floor), Set.of()),
                time.now(),
                nurse.accountId(),
                "Khoanh tầng",
                Set.of(id));

        voidIncident(nurse, id).andExpect(status().isForbidden());
        assertThat(incidentStatus(id)).isEqualTo("IN_PROGRESS");

        containments.release(isolation.id(), isolation.version(), time.now(), nurse.accountId(), "Đã kiểm soát");
    }

    @Test
    void FR_046a_voidingFallKeepsPostFallMonitoringAndReportsIt() throws Exception {
        var result = postNew(
                        nurse,
                        "/api/v1/incidents",
                        "{\"incidentTypeId\":" + typeId("FALL") + ",\"discoveredAt\":\"" + time.now()
                                + "\",\"description\":\"Ngã\",\"level\":\"MEDIUM\",\"lowerLevelReason\":\"Không chấn thương\",\"residentId\":"
                                + resident
                                + ",\"floorAreaId\":" + floor + "}")
                .andExpect(status().isCreated())
                .andReturn();
        long id = Long.parseLong(result.getResponse().getContentAsString().replaceAll("(?s).*\"id\":(\\d+).*", "$1"));

        voidIncident(nurse, id).andExpect(status().isCreated());

        assertThat(incidentStatus(id)).isEqualTo("CANCELLED");
        assertThat(events.stream(EffectiveImpactsRemain.class).filter(event -> event.incidentId() == id))
                .singleElement()
                .satisfies(event -> assertThat(event.impacts())
                        .containsExactly(IncidentCorrectionEffects.END_POST_FALL_MONITORING));
    }

    @Test
    void FR_046a_changingTypeToFallStartsPostFallMonitoringNow() throws Exception {
        long id = incident("MISSING_OR_NOT_RETURNED", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());

        post(
                        nurse,
                        "/api/v1/corrections/M05.INCIDENT/{id}",
                        id,
                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"incidentTypeId\":" + typeId("FALL")
                                + "},\"reason\":\"Thực tế là ngã\"}")
                .andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT type_code FROM incident WHERE id = ?", String.class, id))
                .isEqualTo("FALL");
        assertThat(fallMonitoring.active(resident))
                .hasValueSatisfying(
                        row -> assertThat(row.content().incidentId()).isEqualTo(id));
    }

    private org.springframework.test.web.servlet.ResultActions voidIncident(
            com.eldercare.backend.support.TestUsers.TestUser as, long id) throws Exception {
        return post(as, "/api/v1/corrections/M05.INCIDENT/{id}", id, "{\"type\":\"VOID\",\"reason\":\"Ghi nhầm\"}");
    }
}
