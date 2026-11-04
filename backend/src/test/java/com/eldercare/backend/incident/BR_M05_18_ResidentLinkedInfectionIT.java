package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sự cố lây nhiễm bắt buộc người cao tuổi (BR-M05-18); ghi ngoài phạm vi có {@code out_of_scope} và chỉ trả nhận dạng
 * (002 FR-044a, FR-048, SC-012; T032).
 */
class BR_M05_18_ResidentLinkedInfectionIT extends IncidentHttpTestBase {
    @Autowired
    IncidentStore incidents;

    @Test
    void BR_M05_18_infectionWithoutResidentIsRejected() throws Exception {
        postNew(nurse, "/api/v1/incidents", body("SUSPECTED_INFECTION", null)).andExpect(status().isConflict());
    }

    @Test
    void BR_M05_18_infectionOfResidentInScopeMarksSuspicion() throws Exception {
        var result = postNew(nurse, "/api/v1/incidents", body("SUSPECTED_INFECTION", resident))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificationOnly").value(false))
                .andReturn();
        long id = id(result.getResponse().getContentAsString());

        assertThat(jdbc.queryForObject("SELECT out_of_scope FROM incident WHERE id = ?", Boolean.class, id))
                .isFalse();
        assertThat(incidents.suspectedAt(resident, time.now().plusSeconds(1))).isTrue();
    }

    @Test
    void SC_012_outOfScopeRecorderGetsIdentificationOnly() throws Exception {
        var otherNurse = users.staff(Role.NURSING_STAFF);

        var result = postNew(otherNurse, "/api/v1/incidents", body("FALL", resident))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificationOnly").value(true))
                .andExpect(jsonPath("$.incident.content").doesNotExist())
                .andReturn();
        long id = id(result.getResponse().getContentAsString());

        assertThat(jdbc.queryForObject("SELECT out_of_scope FROM incident WHERE id = ?", Boolean.class, id))
                .isTrue();
    }

    private String body(String type, Long residentId) {
        return "{\"incidentTypeId\":" + typeId(type) + ",\"discoveredAt\":\"" + time.now()
                + "\",\"description\":\"Sốt, ho\",\"level\":\"CRITICAL\",\"floorAreaId\":" + floor
                + (residentId == null ? "" : ",\"residentId\":" + residentId) + "}";
    }

    private static long id(String json) {
        return Long.parseLong(json.replaceAll("(?s).*\"id\":(\\d+).*", "$1"));
    }
}
