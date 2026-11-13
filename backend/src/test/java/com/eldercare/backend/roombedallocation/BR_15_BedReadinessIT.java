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
class BR_15_BedReadinessIT extends StructureTestSupport {
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
    void BR_15_assignedFloorManagerConfirmsAndAppendsOneFact() throws Exception {
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

    @Autowired
    com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi allocationCommands;

    @Autowired
    com.eldercare.backend.support.fake.roombedallocation.FakeAllocationResidencySource residency;
}
