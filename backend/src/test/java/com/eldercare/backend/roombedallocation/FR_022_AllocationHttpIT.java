package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.roombedallocation.api.spi.ResidencyType;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationReminderStore;
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

    @Autowired
    org.springframework.context.ApplicationContext context;

    @Autowired
    com.eldercare.backend.support.fake.administration.FakeDuties duties;

    @Autowired
    com.eldercare.backend.roombedallocation.internal.allocation.service.AllocationReminderProcessor reminders;

    @Autowired
    com.eldercare.backend.common.time.TestClockControl clock;

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

    /** BR-M03-19: trở về khi giường đã giải phóng mang dấu "chưa có giường", nhắc theo CFG-M03-02, phân bổ bắt đầu ngay. */
    @Test
    void BR_M03_19_returnWithoutBedIsFlaggedRemindedAndAllocatedImmediately() throws Exception {
        jdbc.update("UPDATE resident SET status='IN_RESIDENCE' WHERE id=?", resident);
        context.getBean(
                        "allocationStatusParticipant",
                        com.eldercare.backend.resident.api.spi.StatusTransitionParticipant.class)
                .applyEffects(new com.eldercare.backend.resident.api.dto.TransitionResult(
                        resident,
                        com.eldercare.backend.resident.api.dto.ResidentStatus.TEMPORARILY_ABSENT,
                        com.eldercare.backend.resident.api.dto.ResidentStatus.IN_RESIDENCE,
                        com.eldercare.backend.resident.api.dto.StatusCommand.RECORD_RETURN,
                        1,
                        time.now(),
                        tools.jackson.databind.node.JsonNodeFactory.instance.objectNode()));
        mvc.perform(get("/api/v1/residents/" + resident + "/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.withoutBedSince").isNotEmpty());
        var key = new AllocationReminderStore.Key("RESIDENT", resident, "WITHOUT_BED");
        assertThat(reminders.remind(key, "run-1")).isFalse();
        try {
            clock.setTo(time.now().plus(java.time.Duration.ofDays(1)).plusSeconds(60));
            assertThat(reminders.remind(key, "run-2")).isTrue();
            mvc.perform(post("/api/v1/bed-allocations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        } finally {
            clock.reset();
        }
        mvc.perform(get("/api/v1/residents/" + resident + "/location"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.withoutBedSince").doesNotExist())
                .andExpect(jsonPath("$.currentAllocation.location.bedId").value(bed));
    }

    /** BR-M03-20: Trưởng tầng phân bổ ban đầu chỉ xét giường thuộc tầng mình. */
    @Test
    void BR_M03_20_floorManagerInitialAllocationOnlyOnOwnFloorBeds() throws Exception {
        long otherFloor = TestUsers.nextId(), otherRoom = TestUsers.nextId(), otherBed = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)",
                otherFloor,
                area,
                "O" + otherFloor,
                "Tầng khác");
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy,allowed_care_levels) VALUES(?,?,?,?,?,'UNRESTRICTED',JSON_ARRAY('BASIC'))",
                otherRoom,
                otherFloor,
                type,
                "R" + otherRoom,
                "Phòng khác");
        jdbc.update("INSERT INTO bed(id,room_id,code,state) VALUES(?,?,'G1','EMPTY')", otherBed, otherRoom);
        var manager = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
        user.set(manager.user());
        assignments.assign(
                manager.staffId(),
                new com.eldercare.backend.administration.account.api.spi.AssignmentView(
                        com.eldercare.backend.administration.account.api.spi.AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(60),
                        null));
        duties.floorManager(floor, manager.staffId());
        mvc.perform(post("/api/v1/bed-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"residentId\":" + resident + ",\"bedId\":" + otherBed
                                + ",\"reason\":\"Bố trí giường\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/bed-allocations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FUTURE"));
    }
}
