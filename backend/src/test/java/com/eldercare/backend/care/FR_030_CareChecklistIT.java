package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** T038: cùng scope thật và một lần đọc nguồn vị trí/lưu ý theo lô. */
class FR_030_CareChecklistIT extends CareResultTestBase {
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean(name = "fakeShiftStaff")
    com.eldercare.backend.support.fake.care.FakeShiftStaff shiftFixture;

    @Test
    void FR_047a_currentShiftMemberCanClaimCarriedTaskAtCurrentTime() throws Exception {
        caregiver();
        long previous = TestUsers.nextId();
        var at = time.now().minusSeconds(7200);
        shifts.shift(
                new ShiftView(previous, floor, at.minusSeconds(3600), time.now().minusSeconds(3600), true));
        shifts.onShift(performer, at, false);
        shifts.onShift(performer, time.now(), true);
        org.mockito.Mockito.doAnswer(invocation -> !((Instant) invocation.getArgument(1))
                        .isBefore(time.now().minusSeconds(3600)))
                .when(shiftFixture)
                .onShift(org.mockito.ArgumentMatchers.eq(performer), org.mockito.ArgumentMatchers.any());
        jdbc.update(
                "UPDATE care_task SET performer_id=NULL,shift_id=?,planned_at=?,window_from=?,window_to=?,status='OVERDUE',carryover='AWAITING_HANDOVER_CONFIRMATION' WHERE id=?",
                previous,
                local(at),
                local(at.minusSeconds(900)),
                local(at.plusSeconds(900)),
                task);
        mvc.perform(post("/api/v1/care-tasks/" + task + "/claim")
                        .header("Idempotency-Key", "carry-" + TestUsers.nextId()))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT performer_id FROM care_task WHERE id=?", Long.class, task))
                .isEqualTo(performer);
    }

    @Test
    void FR_030_carryoverDoesNotOfferClaimWhenTheCurrentRuleRejectsIt() throws Exception {
        caregiver();
        jdbc.update(
                "UPDATE care_task SET performer_id=NULL,shift_id=?,status='OVERDUE',carryover='FROM_PREVIOUS_SHIFT' WHERE id=?",
                TestUsers.nextId(),
                task);
        mvc.perform(get("/api/v1/checklist").param("shiftId", Long.toString(shift)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.tasks[0].allowedCommands",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("claim"))));
    }

    @Test
    void FR_033_taskDetailKeepsTheRecordingCommandsOfferedByChecklist() throws Exception {
        mvc.perform(get("/api/v1/care-tasks/" + task))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedCommands", org.hamcrest.Matchers.hasItems("record", "notPerform")));
    }

    @Test
    void FR_031_taskDetailOffersClaimToAQualifiedMember() throws Exception {
        caregiver();
        jdbc.update("UPDATE care_task SET performer_id=NULL WHERE id=?", task);
        mvc.perform(get("/api/v1/care-tasks/" + task))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedCommands", org.hamcrest.Matchers.hasItem("claim")));
    }

    @Test
    void FR_030_carryoverBeforeTheCurrentShiftReadWindowStillAppears() throws Exception {
        caregiver();
        var at = time.now().minusSeconds(172800);
        jdbc.update(
                "UPDATE care_task SET performer_id=?,shift_id=?,planned_at=?,window_from=?,window_to=?,status='OVERDUE',carryover='FROM_PREVIOUS_SHIFT' WHERE id=?",
                performer,
                TestUsers.nextId(),
                local(at),
                local(at.minusSeconds(900)),
                local(at.plusSeconds(900)),
                task);
        mvc.perform(get("/api/v1/checklist").param("shiftId", Long.toString(shift)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[0].id").value(task));
    }

    Instant planned() {
        return jdbc.queryForObject("SELECT planned_at FROM care_task WHERE id=?", LocalDateTime.class, task)
                .atZone(BusinessTime.ZONE)
                .toInstant();
    }

    void caregiver() {
        var actor = new TestUsers(jdbc, time).staff(Role.CAREGIVER);
        user.set(actor.user());
        performer = actor.staffId();
        assignments.assign(
                performer,
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        resident,
                        shift,
                        time.now().minusSeconds(3600),
                        time.now().plusSeconds(3600)));
        shifts.members(shift, List.of(new ShiftMemberView(performer, Role.CAREGIVER)));
        shifts.assignments(resident, shift, List.of(new CareAssignmentView(performer, Role.CAREGIVER, true)));
        shifts.onShift(performer, planned(), true);
        shifts.qualified(performer, Set.of(), time.today(), true);
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE',?)",
                type,
                "Loại checklist kiểm thử " + type,
                "{\"group\":\"DRINKING_ASSISTANCE\",\"professional\":false,\"resultType\":\"ML\",\"defaultRole\":\"CAREGIVER\",\"defaultImportance\":\"NORMAL\",\"billable\":false,\"certificates\":[]}");
    }

    @Test
    void FR_030_checklistContainsOwnTasksAndReasonCatalog() throws Exception {
        caregiver();
        jdbc.update("UPDATE care_task SET performer_id=? WHERE id=?", performer, task);
        mvc.perform(get("/api/v1/checklist").param("shiftId", Long.toString(shift)))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.shiftId").value(shift))
                .andExpect(jsonPath("$.loadedAt").exists())
                .andExpect(jsonPath("$.tasks[0].id").value(task))
                .andExpect(jsonPath("$.tasks[0].resultType").value("ML"))
                .andExpect(jsonPath("$.nonPerformanceReasonTypes").isArray());
    }

    @Test
    void FR_030_unassignedTaskIsAbsentFromChecklist() throws Exception {
        caregiver();
        mvc.perform(get("/api/v1/checklist").param("shiftId", Long.toString(shift)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks").isEmpty());
    }

    @Test
    void FR_031_qualifiedMemberCanClaimSharedTask() throws Exception {
        caregiver();
        jdbc.update("UPDATE care_task SET performer_id=NULL WHERE id=?", task);
        mvc.perform(post("/api/v1/care-tasks/" + task + "/claim")
                        .header("Idempotency-Key", "claim-" + TestUsers.nextId()))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT performer_id FROM care_task WHERE id=?", Long.class, task))
                .isEqualTo(performer);
    }

    @Test
    void FR_031_claimCannotStealAnotherStaffMembersTask() throws Exception {
        caregiver();
        mvc.perform(post("/api/v1/care-tasks/" + task + "/claim")
                        .header("Idempotency-Key", "claim-" + TestUsers.nextId()))
                .andExpect(status().isConflict());
    }

    @Test
    void FR_031_missingCertificatePreventsClaim() throws Exception {
        caregiver();
        jdbc.update("UPDATE care_task SET performer_id=NULL WHERE id=?", task);
        jdbc.update(
                "UPDATE catalog_item SET attributes=JSON_SET(attributes,'$.certificates',JSON_ARRAY('CERT_REQUIRED')) WHERE id=?",
                type);
        mvc.perform(post("/api/v1/care-tasks/" + task + "/claim")
                        .header("Idempotency-Key", "claim-" + TestUsers.nextId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_049_doctorMayNotOpenStaffChecklist() throws Exception {
        user.set(new TestUsers(jdbc, time).staff(Role.DOCTOR).user());
        mvc.perform(get("/api/v1/checklist").param("shiftId", Long.toString(shift)))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_030_currentRiskAgreementsAppearOnResidentCards() throws Exception {
        long plan = TestUsers.nextId();
        long risk = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO care_plan(id,resident_id,revision,status,created_by,created_at,effective_from) VALUES(?,?,1,'DRAFT',?,?,?)",
                plan,
                resident,
                user.accountId(),
                local(time.now()),
                time.today());
        jdbc.update(
                "INSERT INTO care_plan_item(id,care_plan_id,kind,data,choice,discussed_risks,resident_participates,assessing_doctor_id,can_self_determine,support_method,assessment_at,assessment_review_date,decision_maker_type,review_date) VALUES(?,?,'RISK_AGREEMENT','{}','Lựa chọn kiểm thử','Rủi ro kiểm thử',TRUE,1,TRUE,'Hỗ trợ kiểm thử',?,?,'RESIDENT',?)",
                risk,
                plan,
                local(time.now()),
                time.today().plusDays(7),
                time.today().plusDays(7));
        jdbc.update("UPDATE care_plan SET status='EFFECTIVE' WHERE id=?", plan);
        mvc.perform(get("/api/v1/checklist").param("shiftId", Long.toString(shift)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskAgreements[0].residentId").value(resident))
                .andExpect(jsonPath("$.riskAgreements[0].agreements[0].choice").value("Lựa chọn kiểm thử"));
    }
}
