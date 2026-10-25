package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.account.api.spi.*;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m03.FakeAllocationInfectionStatus;
import com.cambae.backend.support.fake.m15.*;
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
    com.cambae.backend.support.fake.m03.FakeBedAssets assets;

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

    @Test
    void BR_M03_10_currentInfectionListIsRecheckedAtCommandTime() throws Exception {
        mvc.perform(get("/api/v1/beds/" + bed + "/readiness-conditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disinfectionRequired").value(false));
        infection.listed(departed, time.now().minusSeconds(1), true);
        confirm("{}").andExpect(status().isUnprocessableEntity());
        confirm("{\"disinfected\":true,\"ppeUsed\":false}").andExpect(status().isUnprocessableEntity());
        confirm("{\"disinfected\":true,\"ppeUsed\":true}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT disinfection_required AND disinfected AND ppe_used FROM bed_state_history WHERE bed_id=?",
                        Boolean.class,
                        bed))
                .isTrue();
    }

    @Test
    void FR_044_nursingShiftLeadCanConfirmOwnFloor() throws Exception {
        actor(Role.NURSING_STAFF, true, floor);
        confirm("{}").andExpect(status().isOk());
    }

    @Test
    void FR_044_managerOfficeAndUnassignedFloorManagerCannotConfirm() throws Exception {
        for (var role : new Role[] {Role.NURSING_HOME_MANAGER, Role.OFFICE_STAFF, Role.FLOOR_MANAGER}) {
            actor(role, false, TestUsers.nextId());
            confirm("{}").andExpect(status().is(role == Role.FLOOR_MANAGER ? 404 : 403));
        }
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("AWAITING_READY");
    }

    @Test
    void BR_M03_10_databaseRejectsMissingDisinfectionEvidence() {
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO bed_state_history(bed_id,from_state,to_state,event,occurred_at,basis_type,basis_id,disinfection_required,disinfected,ppe_used) VALUES(?,'AWAITING_READY','EMPTY','CONFIRM_READY',?,'TEST',1,TRUE,FALSE,TRUE)",
                        bed,
                        Timestamp.from(time.now())))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void FR_011a_missingScheduledTransferProviderCannotConfirmBedAsEmpty() throws Exception {
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source,waiting_for_bed_ready,basis_type,basis_id) VALUES(?,?,'FUTURE',?,'BASIC','CURRENT',TRUE,'APPROVAL_REQUEST',?)",
                departed,
                bed,
                Timestamp.from(time.now()),
                TestUsers.nextId());
        confirm("{}").andExpect(status().isServiceUnavailable());
        assertThat(jdbc.queryForObject("SELECT state FROM bed WHERE id=?", String.class, bed))
                .isEqualTo("AWAITING_READY");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bed_state_history WHERE bed_id=?", Integer.class, bed))
                .isZero();
    }

    @Test
    void FR_014_initialFutureReservationPreventsWaitingAdmissionSignal() throws Exception {
        jdbc.update(
                "INSERT INTO bed_allocation(resident_id,bed_id,status,start_at,checked_care_level,care_level_source) VALUES(?,?,'FUTURE',?,'BASIC','CURRENT')",
                departed,
                bed,
                Timestamp.from(time.now().plusSeconds(18000)));
        confirm("{}").andExpect(status().isOk());
        assertThat(events.stream(com.cambae.backend.m03.api.events.BedBecameEmpty.class)
                        .filter(e -> e.bedId() == bed)
                        .count())
                .isZero();
    }

    @Test
    void FR_013b_assetAwaitingTransferPreventsWaitingAdmissionSignal() throws Exception {
        assets.flag(bed, true);
        try {
            confirm("{}").andExpect(status().isOk());
            assertThat(events.stream(com.cambae.backend.m03.api.events.BedBecameEmpty.class)
                            .filter(e -> e.bedId() == bed)
                            .count())
                    .isZero();
        } finally {
            assets.flag(bed, false);
        }
    }
}
