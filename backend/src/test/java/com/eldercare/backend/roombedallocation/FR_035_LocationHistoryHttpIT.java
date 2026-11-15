package com.eldercare.backend.roombedallocation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** T048: giữ dạng phản hồi của contract, không trả DTO nội bộ thay schema web. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
class FR_035_LocationHistoryHttpIT extends StructureTestSupport {
    @Autowired
    MockMvc mvc;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.administration.account.api.PermissionApi permissions;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.resident.api.ResidentStatusApi statuses;

    @Autowired
    com.eldercare.backend.support.fake.administration.FakeDuties duties;

    @Autowired
    com.eldercare.backend.support.fake.administration.FakeAssignments assignments;

    long resident, id;

    @BeforeEach
    void historyHttpFixture() {
        user.set(new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).user());
        resident = TestUsers.nextId();
        id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source,opened_by_id) VALUES(?,?,?,'CLOSED','2026-01-01 00:00:00','2026-01-02 00:00:00','BASIC','CURRENT',?)",
                id,
                resident,
                bed,
                user.accountId());
    }

    @Test
    void FR_035_residentAtAndWindowReturnContractShape() throws Exception {
        mvc.perform(get("/api/v1/location-history/residents/" + resident).param("at", "2026-01-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].allocationId").value(id))
                .andExpect(jsonPath("$[0].location.bedId").value(bed))
                .andExpect(jsonPath("$[0].corrected").value(false));
        mvc.perform(get("/api/v1/location-history/residents/" + resident).param("at", "2026-01-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
