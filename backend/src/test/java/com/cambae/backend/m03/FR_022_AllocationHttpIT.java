package com.cambae.backend.m03;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.m03.api.spi.ResidencyType;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m03.FakeAllocationResidencySource;
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
    com.cambae.backend.support.fake.m15.FakeAssignments assignments;

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

    @Test
    void FR_018_httpReturnsAllViolationsWithEnglishCodes() throws Exception {
        jdbc.update("UPDATE room SET gender_policy='MALE',isolated=TRUE WHERE id=?", room);
        mvc.perform(post("/api/v1/bed-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("UNMET_CONDITIONS"))
                .andExpect(jsonPath(
                        "$.unmetConditions[*].code",
                        org.hamcrest.Matchers.hasItems("ALLOCATION_GENDER", "ALLOCATION_ISOLATION")));
    }

    @Test
    void FR_023_detailCancelAndReasonAreBoundToContract() throws Exception {
        mvc.perform(post("/api/v1/bed-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isCreated());
        long id = jdbc.queryForObject("SELECT id FROM bed_allocation WHERE resident_id=?", Long.class, resident);
        mvc.perform(get("/api/v1/bed-allocations/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residentId").value(resident));
        mvc.perform(post("/api/v1/bed-allocations/" + id + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Thay kế hoạch\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void FR_021_managerCanReadButCannotCreateAllocation() throws Exception {
        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_HOME_MANAGER).user());
        mvc.perform(get("/api/v1/bed-allocations")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/bed-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_011_summaryExcludesSuspendedBedsFromUsableCapacity() throws Exception {
        mvc.perform(get("/api/v1/beds/summary").param("floorId", Long.toString(floor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usable").value(1))
                .andExpect(jsonPath("$.emptyMatching").value(1));
        jdbc.update("UPDATE bed SET state='SUSPENDED',suspension_reason='Kiểm tra' WHERE id=?", bed);
        mvc.perform(get("/api/v1/beds/summary").param("floorId", Long.toString(floor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usable").value(0))
                .andExpect(jsonPath("$.byState.SUSPENDED").value(1));
    }

    @Test
    void FR_025a_locationIncludesWithoutBedMarkerEvenWithoutAnyAllocation() throws Exception {
        jdbc.update(
                "INSERT INTO resident_position(resident_id,without_bed_since) VALUES(?,?)",
                resident,
                Timestamp.from(time.now()));
        mvc.perform(get("/api/v1/residents/" + resident + "/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.withoutBedSince").isNotEmpty());
    }

    @Test
    void FR_037_staffWithoutAllocationViewOnlySeeCurrentLocationWithinScope() throws Exception {
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'ACTIVE',?,'BASIC','CURRENT')",
                resident,
                bed,
                Timestamp.from(time.now().minusSeconds(3600)));
        var doctor = new TestUsers(jdbc, time).staff(Role.DOCTOR).user();
        user.set(doctor);
        assignments.assign(
                doctor.staffId().orElseThrow(),
                new com.cambae.backend.m15.account.api.spi.AssignmentView(
                        com.cambae.backend.m15.account.api.spi.AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        resident,
                        null,
                        time.now().minusSeconds(3600),
                        null));
        mvc.perform(get("/api/v1/residents/" + resident + "/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.bedId").value(bed))
                .andExpect(jsonPath("$.currentAllocation").doesNotExist())
                .andExpect(jsonPath("$.futureAllocation").doesNotExist());
    }
}
