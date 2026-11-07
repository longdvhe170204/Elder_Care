package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.incident.api.events.IsolationLifted;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * BR-M05-12, FR-064, FR-066 (US8 #6, #7): chỉ Bác sĩ, Quản lý viện gỡ vùng, bắt buộc lý do; gỡ thì bỏ chặn; sự cố lây
 * nhiễm không đóng được khi còn vùng chỉ gắn với nó, gắn vùng với sự cố lây nhiễm khác chưa đóng thì đóng được.
 */
@RecordApplicationEvents
class BR_M05_12_LiftIsolationIT extends IncidentHttpTestBase {
    private static final String CLOSE = "/api/v1/incidents/{id}/close";
    private static final String CLOSE_BODY = "{\"resolution\":\"Đã loại trừ\",\"infectionOutcome\":\"RULED_OUT\"}";

    @Autowired
    ApplicationEvents events;

    @Autowired
    AllocationInfectionStatus infections;

    TestUser doctor;
    long infection;
    long zone;

    @BeforeEach
    void isolatedFloor() throws Exception {
        doctor = users.staff(Role.DOCTOR);
        long area = TestUsers.nextId();
        jdbc.update("INSERT INTO area(id,code,name) VALUES (?,?,?)", area, "A" + area, "Khu kiểm thử");
        jdbc.update("INSERT INTO floor(id,area_id,code,name) VALUES (?,?,?,?)", floor, area, "F" + floor, "Tầng 2");
        infection = incident("SUSPECTED_INFECTION", "MEDIUM", "IN_PROGRESS", nurse.accountId(), nurse.accountId());
        postNew(doctor, "/api/v1/isolations", isolation(infection)).andExpect(status().isCreated());
        zone = jdbc.queryForObject(
                "SELECT MAX(id) FROM simple_record WHERE kind = 'M05.CONTAINMENT'"
                        + " AND JSON_CONTAINS(JSON_EXTRACT(content, '$.incidentIds'), CAST(? AS JSON))",
                Long.class,
                Long.toString(infection));
    }

    @Test
    void BR_M05_12_onlyDoctorOrManagerLiftsWithReason() throws Exception {
        post(floorManager, "/api/v1/isolations/{id}/lift", zone, "{\"reason\":\"Hết ca bệnh\"}")
                .andExpect(status().isForbidden());
        post(doctor, "/api/v1/isolations/{id}/lift", zone, "{}").andExpect(status().isUnprocessableContent());
        assertThat(infections.cordonedAt(time.now()).floorIds()).contains(floor);

        post(doctor, "/api/v1/isolations/{id}/lift", zone, "{\"reason\":\"Hết thời gian theo dõi, không ca mới\"}")
                .andExpect(status().isOk());

        assertThat(infections.cordonedAt(time.now()).floorIds()).doesNotContain(floor);
        assertThat(events.stream(IsolationLifted.class)).anyMatch(e -> e.zoneId() == zone);
    }

    @Test
    void FR_066_infectionCannotCloseWhileZoneOnlyAttachedToIt() throws Exception {
        post(nurse, CLOSE, infection, CLOSE_BODY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ISOLATION_ONLY_FOR_THIS_INCIDENT"));
        assertThat(incidentStatus(infection)).isEqualTo("IN_PROGRESS");

        long other = incident("SUSPECTED_INFECTION", "MEDIUM", "NEW", null, nurse.accountId());
        post(doctor, "/api/v1/isolations/{id}/attach-incident", zone, "{\"incidentId\":" + other + "}")
                .andExpect(status().isOk());

        post(nurse, CLOSE, infection, CLOSE_BODY).andExpect(status().isOk());
        assertThat(incidentStatus(infection)).isEqualTo("CLOSED");
        assertThat(infections.cordonedAt(time.now()).floorIds()).contains(floor);
    }

    private String isolation(long incidentId) {
        return "{\"scope\":{\"floorIds\":[" + floor + "]},\"incidentIds\":[" + incidentId
                + "],\"reason\":\"Ổ cúm tầng 2\"}";
    }
}
