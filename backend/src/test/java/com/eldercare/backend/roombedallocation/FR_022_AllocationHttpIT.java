package com.eldercare.backend.roombedallocation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** T027: HTTP theo đúng hợp đồng Phase 4 đã duyệt, quyền thật và cùng checker. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class FR_022_AllocationHttpIT extends StructureTestSupport {
    @Autowired
    com.eldercare.backend.support.fake.administration.FakeAssignments assignments;

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeAllocationResidencySource residency;

    long resident;

    @BeforeEach
    void residentFixture() {
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','ADMISSION_IN_PROGRESS',?,'BASIC',?,?)",
                resident,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        residency.resident(resident, ResidencyType.LONG_TERM, null, null, null, null);
    }

    private String body() {
        return "{\"residentId\":" + resident + ",\"bedId\":" + bed + ",\"reason\":\"Bố trí giường\"}";
    }

    @Test
    void FR_022_listAndCreateUseSameConditions() throws Exception {
        mvc.perform(get("/api/v1/bed-allocations/suitable-beds").param("residentId", Long.toString(resident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.location.bedId == " + bed + ")]").isNotEmpty());
        mvc.perform(post("/api/v1/bed-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FUTURE"))
                .andExpect(jsonPath("$.checkedCareLevel").value("BASIC"))
                .andExpect(jsonPath("$.careLevelSource").value("CURRENT"));
        mvc.perform(get("/api/v1/bed-allocations")
                        .param("residentId", Long.toString(resident))
                        .param("status", "FUTURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].location.bedId").value(bed));
        mvc.perform(get("/api/v1/residents/" + resident + "/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.futureAllocation.location.bedId").value(bed));
    }
}
