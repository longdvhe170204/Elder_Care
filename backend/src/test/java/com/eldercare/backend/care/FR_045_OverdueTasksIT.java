package com.eldercare.backend.care;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

class FR_045_OverdueTasksIT extends CareOverdueTestBase {
    @Test
    void FR_045_floorManagerSeesOnlyOverdueTasksInScope() throws Exception {
        long id = create();
        jdbc.update("UPDATE care_task SET status='OVERDUE' WHERE id=?", id);
        mvc.perform(get("/api/v1/care-tasks/overdue").param("floorAreaId", Long.toString(floor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].status").value("OVERDUE"));
        mvc.perform(get("/api/v1/care-tasks/overdue").param("floorAreaId", Long.toString(floor + 1)))
                .andExpect(status().isForbidden());
    }
}
