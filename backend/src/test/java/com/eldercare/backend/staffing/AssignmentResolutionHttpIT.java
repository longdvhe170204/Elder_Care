package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.support.fake.admission.FakeContractSchedules;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class AssignmentResolutionHttpIT extends AssignmentHttpTestBase {
    @Autowired
    FakeContractSchedules attendance;

    @AfterEach
    void clearAttendance() {
        attendance.clear();
    }

    private MockHttpServletRequestBuilder resolution(Instant at) {
        return get("/api/v1/shifts/" + shift + "/assignment-resolution")
                .header("Authorization", bearer)
                .param("residentId", Long.toString(resident))
                .param("at", at.toString())
                .param("role", "CAREGIVER");
    }

    private MockHttpServletRequestBuilder missing() {
        return get("/api/v1/shifts/" + shift + "/unassigned-residents").header("Authorization", bearer);
    }

    @Test
    void FR_028_moreSpecificAndTaskSpecificAssignmentsWin() throws Exception {
        assign(other.staffId(), "ROOM", room, "CAREGIVER", "PRIMARY", false, 123L);
        publish();
        mvc.perform(resolution(from))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryStaffId").value(worker.staffId()));
        mvc.perform(resolution(from).param("taskTypeId", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryStaffId").value(other.staffId()));
    }
}
