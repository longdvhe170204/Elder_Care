package com.eldercare.backend.roombedallocation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@WithMockUser
class FR_016_BedHistoryHttpIT extends StructureTestSupport {
    @Autowired
    MockMvc mvc;

    @Test
    void FR_011_commandsAndHistoryUseTheApprovedContract() throws Exception {
        mvc.perform(post("/api/v1/beds/" + bed + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Kiểm tra phòng\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SUSPENDED"));
        mvc.perform(post("/api/v1/beds/" + bed + "/reuse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Mở lại phòng\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("AWAITING_READY"));
        mvc.perform(get("/api/v1/beds/" + bed + "/state-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].event").value("SUSPEND"))
                .andExpect(jsonPath("$[0].reason").value("Kiểm tra phòng"))
                .andExpect(jsonPath("$[1].to").value("AWAITING_READY"));
    }

    @Test
    void FR_016_historyIsBoundToTheRequestedBedAndCursor() throws Exception {
        jdbc.update(
                "INSERT INTO bed_state_history(bed_id,to_state,event,occurred_at,floor_area_id,basis_type,basis_id) VALUES(?,'EMPTY','CREATE_BED','2026-01-01',?,'BED',1)",
                bed,
                floor);
        long id = jdbc.queryForObject("SELECT MAX(id) FROM bed_state_history WHERE bed_id=?", Long.class, bed);
        mvc.perform(get("/api/v1/beds/" + bed + "/state-history").param("after", Long.toString(id)))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void FR_011_missingReasonReturns422() throws Exception {
        mvc.perform(post("/api/v1/beds/" + bed + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
    }

    @Test
    void FR_011_floorManagerCannotSuspend() throws Exception {
        user.set(new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER).user());
        mvc.perform(post("/api/v1/beds/" + bed + "/suspend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Kiểm tra\"}"))
                .andExpect(status().isForbidden());
    }
}
