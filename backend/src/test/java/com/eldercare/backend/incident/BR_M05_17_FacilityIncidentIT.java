package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/** Sự cố cơ sở vật chất không gắn người cao tuổi, bắt buộc tầng/khu vực (BR-M05-17, FR-040; T032). */
class BR_M05_17_FacilityIncidentIT extends IncidentHttpTestBase {

    @Test
    void BR_M05_17_facilityIncidentWithoutFloorIsRejected() throws Exception {
        int before = count();
        postNew(floorManager, "/api/v1/incidents", body(null)).andExpect(status().isConflict());
        assertThat(count()).isEqualTo(before);
    }

    @Test
    void BR_M05_17_facilityIncidentWithFloorHasNoResident() throws Exception {
        var result = postNew(floorManager, "/api/v1/incidents", body(floor))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.identificationOnly").value(false))
                .andReturn();
        long id = Long.parseLong(result.getResponse().getContentAsString().replaceAll("(?s).*\"id\":(\\d+).*", "$1"));

        var row = jdbc.queryForMap("SELECT resident_id, floor_area_id, type_code FROM incident WHERE id = ?", id);
        assertThat(row.get("resident_id")).isNull();
        assertThat(((Number) row.get("floor_area_id")).longValue()).isEqualTo(floor);
        assertThat(row.get("type_code")).isEqualTo("FACILITY");
    }

    private String body(Long floorAreaId) {
        return "{\"incidentTypeId\":" + typeId("FACILITY") + ",\"discoveredAt\":\"" + time.now()
                + "\",\"description\":\"Vỡ ống nước hành lang\",\"level\":\"MEDIUM\""
                + (floorAreaId == null ? "" : ",\"floorAreaId\":" + floorAreaId)
                + ",\"lowerLevelReason\":\"Không ảnh hưởng người cao tuổi\"}";
    }

    private int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM incident WHERE type_code = 'FACILITY'", Integer.class);
    }
}
