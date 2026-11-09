package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeAllocationInfectionStatus;
import java.sql.Timestamp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** T057: quyền thật002, nguồn lây nhiễm giả đúng chủ, constraint MySQL thật. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@org.springframework.test.context.event.RecordApplicationEvents
class BR_M03_09_BedReadinessIT extends StructureTestSupport {
    @Autowired
    org.springframework.test.context.event.ApplicationEvents events;

    @Autowired
    com.eldercare.backend.support.fake.roombedallocation.FakeBedAssets assets;

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeDuties duties;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeAllocationInfectionStatus infection;

    long departed, closed;

    @BeforeEach
    void awaiting() {
        infection.reset();
        departed = TestUsers.nextId();
        closed = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES(?,'[Họ tên NCT 1]','1950-01-01','FEMALE','IN_RESIDENCE',?,'BASIC',?,?)",
                departed,
                Timestamp.from(time.now()),
                Timestamp.from(time.now()),
                user.accountId());
        jdbc.update(
                "INSERT INTO bed_allocation(id,resident_id,bed_id,status,start_at,end_at,checked_care_level,care_level_source) VALUES(?,?,?,'CLOSED',?,?,'BASIC','CURRENT')",
                closed,
                departed,
                bed,
                Timestamp.from(time.now().minusSeconds(7200)),
                Timestamp.from(time.now().minusSeconds(3600)));
        jdbc.update(
                "UPDATE bed SET state='AWAITING_READY',awaiting_ready_since=?,ready_deadline=?,last_closed_allocation_id=? WHERE id=?",
                Timestamp.from(time.now().minusSeconds(3600)),
                Timestamp.from(time.now().plusSeconds(10800)),
                closed,
                bed);
        actor(Role.FLOOR_MANAGER, false, floor);
    }

    private void actor(Role role, boolean lead, long assignedFloor) {
        var actor = new TestUsers(jdbc, time).staff(role);
        user.set(actor.user());
        assignments.assign(
                actor.staffId(),
                new AssignmentView(
                        lead ? AssignmentKind.SHIFT_LEAD : AssignmentKind.FLOOR_MANAGER,
                        assignedFloor,
                        null,
                        null,
                        lead ? 99L : null,
                        time.now().minusSeconds(60),
                        time.now().plusSeconds(3600)));
        if (lead) duties.shiftLead(assignedFloor, actor.staffId());
        else if (role == Role.FLOOR_MANAGER) duties.floorManager(assignedFloor, actor.staffId());
    }

    private org.springframework.test.web.servlet.ResultActions confirm(String body) throws Exception {
        return mvc.perform(post("/api/v1/beds/" + bed + "/confirm-readiness")
                .contentType("application/json")
                .content(body));
    }

    @Test
    void BR_M03_09_assignedFloorManagerConfirmsAndAppendsOneFact() throws Exception {
        mvc.perform(get("/api/v1/beds/" + bed + "/readiness-conditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.disinfectionRequired").value(false));
        confirm("{\"note\":\"Đã kiểm tra\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("EMPTY"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_state_history WHERE bed_id=? AND event='CONFIRM_READY' AND note='Đã kiểm tra'",
                        Integer.class,
                        bed))
                .isEqualTo(1);
        confirm("{}").andExpect(status().isConflict());
    }

    /**
     * T060 (FR-044, research G-14; GET /bed-readiness/beds duyệt 2026-11-06): Điều dưỡng giữ nhiệm vụ Người phụ trách
     * ca thấy giường Chờ sẵn sàng của tầng đang phụ trách; hết nhiệm vụ hoặc không có nhiệm vụ thì 403; Trưởng tầng
     * theo quyền cũ; không lộ giường tầng khác, floorId chỉ thu hẹp.
     */
    @Test
    void FR_044_readinessListFollowsShiftLeadDutyAndFloor() throws Exception {
        long otherFloor = TestUsers.nextId(), otherRoom = TestUsers.nextId(), otherBed = TestUsers.nextId();
        long emptyBed = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO floor(id,area_id,code,name) VALUES(?,?,?,?)",
                otherFloor,
                area,
                "O" + otherFloor,
                "Tầng khác");
        jdbc.update(
                "INSERT INTO room(id,floor_id,room_type_id,code,name,gender_policy) VALUES(?,?,?,?,?,'UNRESTRICTED')",
                otherRoom,
                otherFloor,
                type,
                "R" + otherRoom,
                "Phòng khác");
        jdbc.update(
                "INSERT INTO bed(id,room_id,code,state,awaiting_ready_since,ready_deadline) VALUES(?,?,'G1','AWAITING_READY',?,?)",
                otherBed,
                otherRoom,
                Timestamp.from(time.now().minusSeconds(3600)),
                Timestamp.from(time.now().plusSeconds(3600)));
        jdbc.update("INSERT INTO bed(id,room_id,code) VALUES(?,?,'G9')", emptyBed, room);

        actor(Role.NURSING_STAFF, true, floor);
        mvc.perform(get("/api/v1/bed-readiness/beds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + bed + ")].state")
                        .value(org.hamcrest.Matchers.contains("AWAITING_READY")))
                .andExpect(jsonPath("$[?(@.id==" + bed + ")].location.floorId")
                        .value(org.hamcrest.Matchers.contains((int) floor)))
                .andExpect(jsonPath("$[?(@.id==" + bed + ")].readinessOverdue")
                        .value(org.hamcrest.Matchers.contains(false)))
                .andExpect(jsonPath("$[?(@.id==" + otherBed + ")]").isEmpty())
                .andExpect(jsonPath("$[?(@.id==" + emptyBed + ")]").isEmpty());
        mvc.perform(get("/api/v1/bed-readiness/beds").param("floorId", Long.toString(otherFloor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        duties.shiftLead(floor, TestUsers.nextId());
        mvc.perform(get("/api/v1/bed-readiness/beds")).andExpect(status().isForbidden());

        user.set(new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user());
        mvc.perform(get("/api/v1/bed-readiness/beds")).andExpect(status().isForbidden());

        jdbc.update(
                "UPDATE bed SET ready_deadline=? WHERE id=?",
                Timestamp.from(time.now().minusSeconds(60)),
                bed);
        actor(Role.FLOOR_MANAGER, false, floor);
        mvc.perform(get("/api/v1/bed-readiness/beds").param("floorId", Long.toString(floor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + bed + ")].readinessOverdue")
                        .value(org.hamcrest.Matchers.contains(true)))
                .andExpect(jsonPath("$[?(@.id==" + otherBed + ")]").isEmpty());
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("AWAITING_READY");
    }

    @Autowired
    com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi allocationCommands;

    @Autowired
    com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource residency;
}
