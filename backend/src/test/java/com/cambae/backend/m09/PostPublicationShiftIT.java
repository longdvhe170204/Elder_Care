package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.*;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.m15.account.api.FieldVisibilityApi;
import com.cambae.backend.m15.account.api.dto.*;
import com.cambae.backend.support.*;
import com.cambae.backend.support.fake.m02.FakeM02Config;
import com.cambae.backend.support.fake.m03.*;
import com.cambae.backend.support.fake.m04.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/** T039: HTTP/quyền thật, chỉ nguồn chưa có của B/C dùng bản giả chung. */
@AutoConfigureMockMvc
@Import({FakeM02Config.class, FakeM03Config.class, FakeM04Config.class, PostPublicationShiftIT.Time.class})
class PostPublicationShiftIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeLocations locations;

    @Autowired
    FakeCareRequirements tasks;

    @Autowired
    FieldVisibilityApi visibility;

    @Autowired
    com.cambae.backend.m15.parameter.api.ParameterApi parameters;

    private final Map<String, tools.jackson.databind.JsonNode> originalParameters = new LinkedHashMap<>();

    final Instant from = Instant.parse("2026-10-25T00:00:00Z"), to = from.plusSeconds(28800);
    TestUsers users;
    TestUsers.TestUser manager, head, lead, worker, replacement, outside;
    long floor, shift, member, assignment;
    String managerToken, headToken, leadToken, workerToken;

    private TestUsers.TestUser person(Role role) {
        long used = jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM staff_profile", Long.class);
        while (TestUsers.nextId() <= used) {
            /* Giữ id không trùng các hồ sơ import ở test trước. */
        }
        var user = users.staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên kiểm thử lệnh ca]','Chăm sóc',?)",
                user.staffId(),
                user.accountId());
        return user;
    }

    @BeforeEach
    void setup() {
        clock.setTo(from.plusSeconds(1200));
        users = new TestUsers(jdbc, time);
        manager = person(Role.NURSING_HOME_MANAGER);
        head = person(Role.FLOOR_MANAGER);
        lead = person(Role.NURSING_STAFF);
        worker = person(Role.CAREGIVER);
        replacement = person(Role.NURSING_STAFF);
        outside = person(Role.FLOOR_MANAGER);
        managerToken = token(manager);
        headToken = token(head);
        leadToken = token(lead);
        workerToken = token(worker);
        floor = TestUsers.nextId();
        locations.floor(floor, null);
        long template = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Mẫu lệnh ca]','07:00','15:00','FLOOR',TRUE)");
        long schedule = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_schedule(month,scope_type,scope_id,status) VALUES ('2026-10','FLOOR',?,'PUBLISHED')",
                floor);
        shift = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status,lead_staff_id) VALUES (?,?,?,?,'FLOOR',?,TRUE,'IN_PROGRESS',?)",
                schedule,
                template,
                ShiftStore.sql(from),
                ShiftStore.sql(to),
                floor,
                lead.staffId());
        jdbc.update(
                "INSERT INTO floor_manager_assignment(staff_id,floor_area_id,starts_at) VALUES (?,?,?)",
                head.staffId(),
                floor,
                ShiftStore.sql(from.minusSeconds(86400)));
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, lead.staffId());
        member = AssignmentFixtures.insert(
                jdbc, "INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, worker.staffId());
        jdbc.update(
                "INSERT INTO shift_lead_history(shift_id,staff_id,starts_at,basis) VALUES (?,?,?,'SCHEDULED')",
                shift,
                lead.staffId(),
                ShiftStore.sql(from));
        assignment = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,starts_at,ends_at,status) VALUES (?,?,'FLOOR',?,'CAREGIVER','PRIMARY',?,?,'ACTIVE')",
                shift,
                worker.staffId(),
                floor,
                ShiftStore.sql(from),
                ShiftStore.sql(to));
        tasks.onAbsent((staff, ca) -> {});
        tasks.failOnEnd(false);
    }

    @AfterEach
    void reset() throws Exception {
        tasks.onAbsent((staff, ca) -> {});
        for (var original : originalParameters.entrySet())
            changeParameter(
                    original.getKey(),
                    original.getValue().isNull()
                            ? Map.of("restoreDefault", true)
                            : Map.of("value", original.getValue()));
        originalParameters.clear();
        clock.reset();
    }

    private void parameter(String code, Object value) throws Exception {
        originalParameters.put(
                code,
                parameters
                        .read(com.cambae.backend.m15.parameter.api.ParameterCode.valueOf(code))
                        .raw());
        changeParameter(code, Map.of("value", value));
    }

    private void changeParameter(String code, Map<String, Object> value) throws Exception {
        clock.advanceBy(Duration.ofMillis(1));
        var body = new HashMap<String, Object>(value);
        body.put("reason", "[Cấu hình tỷ lệ kiểm thử]");
        var apiCode = code.replace('_', '-');
        long version = jdbc.queryForObject("SELECT version FROM parameter WHERE code=?", Long.class, apiCode);
        mvc.perform(post("/api/v1/parameters/{code}/change-value", apiCode)
                        .header("Authorization", token(manager))
                        .header("If-Match", "\"" + version + "\"")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private String token(TestUsers.TestUser user) {
        return TestUsers.bearer(users.session(user.accountId()));
    }

    private ResultActions command(String action, String token, Map<String, Object> body) throws Exception {
        return mvc.perform(post("/api/v1/shifts/{id}/" + action, shift)
                .header("Authorization", token)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private Map<String, Object> reason(long staff) {
        return Map.of("staffId", staff, "reason", "[Lý do riêng tư kiểm thử]");
    }

    @Test
    void FR_020_absenceEndsOnlyThisShiftAssignments_andCallsTasksInSameTransaction() throws Exception {
        parameter(
                "CFG_M09_01",
                Map.of(
                        jdbc.queryForObject("SELECT template_id FROM work_shift WHERE id=?", Long.class, shift)
                                .toString(),
                        8));
        parameter("CFG_M09_12", Map.of("BASIC", 1, "REGULAR", 1.5, "SPECIAL", 2.5));
        var called = new AtomicInteger();
        tasks.onAbsent((staff, ca) -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .isTrue();
            assertThat(staff).isEqualTo(worker.staffId());
            assertThat(ca).isEqualTo(shift);
            called.incrementAndGet();
        });
        command("record-absence", leadToken, reason(worker.staffId())).andExpect(status().isOk());
        assertThat(called).hasValue(1);
        assertThat(jdbc.queryForObject("SELECT absent_at FROM shift_staff WHERE id=?", Object.class, member))
                .isNotNull();
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assignment))
                .isEqualTo("ENDED");
        assertThat(jdbc.queryForObject(
                        "SELECT actual_end_at FROM staff_assignment WHERE id=?", LocalDateTime.class, assignment))
                .isEqualTo(LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staffing_ratio_calculation WHERE shift_id=? AND event='STAFF_ABSENT'",
                        Integer.class,
                        shift))
                .isEqualTo(1);
        command("record-absence", leadToken, reason(worker.staffId())).andExpect(status().isConflict());
    }

    @Test
    void FR_020_taskFailureRollsBackAbsenceAssignmentsAuditAndRatio() throws Exception {
        var called = new AtomicInteger();
        tasks.onAbsent((staff, ca) -> {
            called.incrementAndGet();
            throw new IllegalStateException("[Nguồn công việc lỗi kiểm thử]");
        });
        try {
            command("record-absence", headToken, reason(worker.staffId()));
        } catch (jakarta.servlet.ServletException expected) {
        }
        assertThat(called).hasValue(1);
        assertThat(jdbc.queryForObject("SELECT absent_at FROM shift_staff WHERE id=?", Object.class, member))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assignment))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.STAFF_ABSENCE' AND entity_id=?",
                        Integer.class,
                        Long.toString(member)))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM staffing_ratio_calculation WHERE shift_id=?", Integer.class, shift))
                .isZero();
    }

    @Test
    void FR_020_onlyAssignedHeadOrCurrentLeadCanRecordAbsence_andReasonIsRequired() throws Exception {
        command("record-absence", workerToken, reason(worker.staffId())).andExpect(status().isForbidden());
        command("record-absence", token(outside), reason(worker.staffId())).andExpect(status().isNotFound());
        command("record-absence", headToken, Map.of("staffId", worker.staffId()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void FR_020_reportingWindowRejectsEarlyAndLate_andAcceptsEndInstant() throws Exception {
        clock.setTo(from.minus(parameters
                        .read(com.cambae.backend.m15.parameter.api.ParameterCode.CFG_M15_07)
                        .asDuration())
                .minusSeconds(1));
        command("record-absence", token(head), reason(worker.staffId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("OUTSIDE_ABSENCE_REPORTING_WINDOW"));
        clock.setTo(to.plusSeconds(1));
        command("record-absence", token(head), reason(worker.staffId())).andExpect(status().isConflict());
        clock.setTo(to);
        command("record-absence", token(head), reason(worker.staffId())).andExpect(status().isOk());
    }

    @Test
    void FR_019_leadChangeClosesHistoryTransfersDraft_andPreservesAssessment() throws Exception {
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, replacement.staffId());
        long handover = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO shift_handover(shift_id,handover_staff_id,created_at,status) VALUES (?,?,?,'DRAFT')",
                shift,
                lead.staffId(),
                ShiftStore.sql(time.now()));
        jdbc.update(
                "INSERT INTO handover_assessment(handover_id,assessment_kind,content) VALUES (?,'GENERAL','[Nhận định được giữ]')",
                handover);
        jdbc.update(
                "INSERT INTO handover_item(handover_id,item_group,room_id,description,note,note_author,created_at) VALUES (?,'FOLLOW_UP_ISSUE',?,'[Mục được giữ]','[Ghi chú cũ]','[Người viết cũ]',?)",
                handover,
                TestUsers.nextId(),
                ShiftStore.sql(time.now()));
        command("change-lead", headToken, reason(replacement.staffId())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT lead_staff_id FROM work_shift WHERE id=?", Long.class, shift))
                .isEqualTo(replacement.staffId());
        assertThat(jdbc.queryForObject("SELECT handover_staff_id FROM shift_handover WHERE id=?", Long.class, handover))
                .isEqualTo(replacement.staffId());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM shift_lead_history WHERE shift_id=? AND ends_at IS NULL",
                        Integer.class,
                        shift))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT content FROM handover_assessment WHERE handover_id=?", String.class, handover))
                .isEqualTo("[Nhận định được giữ]");
        assertThat(jdbc.queryForObject(
                        "SELECT note_author FROM handover_item WHERE handover_id=?", String.class, handover))
                .isEqualTo("[Người viết cũ]");
        command("record-absence", leadToken, reason(worker.staffId())).andExpect(status().isNotFound());
        command("record-absence", token(replacement), reason(worker.staffId())).andExpect(status().isOk());
    }

    @Test
    void FR_020_absentLeadNeedsReplacementWhenEligibleMemberExists() throws Exception {
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, replacement.staffId());
        command("record-absence", headToken, reason(lead.staffId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REPLACEMENT_LEAD_REQUIRED"));
        var body = new HashMap<>(reason(lead.staffId()));
        body.put("replacementLeadStaffId", replacement.staffId());
        command("record-absence", headToken, body).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT lead_staff_id FROM work_shift WHERE id=?", Long.class, shift))
                .isEqualTo(replacement.staffId());
    }

    @Test
    void FR_053_reasonOnlyVisibleToRecorderAssignedHeadAndManager() throws Exception {
        command("record-absence", leadToken, reason(worker.staffId())).andExpect(status().isOk());
        var target = new Target("M09.STAFF_ABSENCE", Long.toString(member), null, floor);
        assertThat(visibility.canView(lead.user(), InfoGroup.STAFF_REASON, target))
                .isEqualTo(Visibility.FULL);
        assertThat(visibility.canView(head.user(), InfoGroup.STAFF_REASON, target))
                .isEqualTo(Visibility.FULL);
        assertThat(visibility.canView(manager.user(), InfoGroup.STAFF_REASON, target))
                .isEqualTo(Visibility.FULL);
        assertThat(visibility.canView(worker.user(), InfoGroup.STAFF_REASON, target))
                .isEqualTo(Visibility.NONE);
        assertThat(visibility.canView(outside.user(), InfoGroup.STAFF_REASON, target))
                .isEqualTo(Visibility.NONE);
    }

    @Test
    void FR_018_addStaffChecksMembershipAndReturnsCommandsFromBackend() throws Exception {
        command("add-staff", headToken, reason(replacement.staffId())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM shift_staff WHERE shift_id=? AND staff_id=? AND added_at IS NOT NULL",
                        Integer.class,
                        shift,
                        replacement.staffId()))
                .isEqualTo(1);
        command("add-staff", headToken, reason(replacement.staffId())).andExpect(status().isConflict());
        command("add-staff", leadToken, reason(outside.staffId())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/shifts/{id}", shift).header("Authorization", headToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedCommands").value(org.hamcrest.Matchers.hasItem("addStaff")));
    }

    @Test
    void FR_021_cancelOnlyManagerBeforeStart_retainsMembershipAndCancelsAssignments() throws Exception {
        command("cancel", managerToken, Map.of("reason", "[Hủy ca kiểm thử]")).andExpect(status().isConflict());
        clock.setTo(from.minusSeconds(3600));
        jdbc.update("UPDATE work_shift SET status='PUBLISHED' WHERE id=?", shift);
        command("cancel", token(head), Map.of("reason", "[Hủy ca kiểm thử]")).andExpect(status().isForbidden());
        command("cancel", token(manager), Map.of("reason", "[Hủy ca kiểm thử]")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM work_shift WHERE id=?", String.class, shift))
                .isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shift_staff WHERE shift_id=?", Integer.class, shift))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, assignment))
                .isEqualTo("CANCELLED");
    }

    @Test
    void FR_053_privateAbsenceReasonIsNotCopiedIntoAssignmentHistoryOrNotifications() throws Exception {
        command("record-absence", leadToken, reason(worker.staffId())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT end_reason FROM staff_assignment WHERE id=?", String.class, assignment))
                .doesNotContain("[Lý do riêng tư kiểm thử]");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type<>'M09.STAFF_ABSENCE' AND JSON_EXTRACT(details,'$.shiftId')=? AND reason='[Lý do riêng tư kiểm thử]'",
                        Integer.class,
                        shift))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM event_publication WHERE serialized_event LIKE '%Lý do riêng tư kiểm thử%'",
                        Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(data_after,'$.reason')) FROM audit_log WHERE entity_type='M09.STAFF_ABSENCE' AND entity_id=?",
                        String.class,
                        Long.toString(member)))
                .isEqualTo("[Lý do riêng tư kiểm thử]");
    }

    @Test
    void FR_020_absentLeadWithoutEligibleReplacementMarksMissingLead_andHeadKeepsControl() throws Exception {
        command("record-absence", headToken, reason(lead.staffId())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT missing_lead FROM work_shift WHERE id=?", Boolean.class, shift))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT lead_staff_id FROM work_shift WHERE id=?", Long.class, shift))
                .isNull();
        command("record-absence", headToken, reason(worker.staffId())).andExpect(status().isOk());
    }

    @Test
    void FR_056_dutyHistoryIsManagerOnlyFilteredByFloorAndContainsNoPrivateReason() throws Exception {
        command("record-absence", leadToken, reason(worker.staffId())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/duty-basis-audit-logs").header("Authorization", headToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/duty-basis-audit-logs")
                        .param("floorAreaId", Long.toString(floor))
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .header("Authorization", managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.basis == 'SHIFT_LEAD')]").isNotEmpty())
                .andExpect(content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Lý do riêng tư"))));
        mvc.perform(get("/api/v1/duty-basis-audit-logs")
                        .param("floorAreaId", Long.toString(TestUsers.nextId()))
                        .header("Authorization", managerToken))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void FR_018_overlappingAdditionIsRejected_withoutChangingShift() throws Exception {
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, replacement.staffId());
        long second = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) SELECT schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,FALSE,'PUBLISHED' FROM work_shift WHERE id=?",
                shift);
        var result = mvc.perform(post("/api/v1/shifts/{id}/add-staff", second)
                .header("Authorization", headToken)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(reason(replacement.staffId()))));
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SHIFT_OVERLAP"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shift_staff WHERE shift_id=?", Integer.class, second))
                .isZero();
    }

    @Test
    void FR_020_twoConcurrentAbsenceCommandsApplyExactlyOnce() throws Exception {
        var latch = new java.util.concurrent.CountDownLatch(1);
        var calls = new AtomicInteger();
        tasks.onAbsent((staff, ca) -> calls.incrementAndGet());
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> {
                latch.await();
                return command("record-absence", headToken, reason(worker.staffId()))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            var b = pool.submit(() -> {
                latch.await();
                return command("record-absence", headToken, reason(worker.staffId()))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            latch.countDown();
            assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(calls).hasValue(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.STAFF_ABSENCE' AND entity_id=?",
                        Integer.class,
                        Long.toString(member)))
                .isEqualTo(1);
    }

    @Test
    void FR_020_absenceLeavesOtherShiftAndItsAssignmentsUnchanged() throws Exception {
        long other = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO work_shift(schedule_id,template_id,starts_at,ends_at,scope_type,scope_id,requires_handover,status) SELECT schedule_id,template_id,DATE_ADD(starts_at,INTERVAL 1 DAY),DATE_ADD(ends_at,INTERVAL 1 DAY),scope_type,scope_id,FALSE,'PUBLISHED' FROM work_shift WHERE id=?",
                shift);
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", other, worker.staffId());
        long otherAssignment = AssignmentFixtures.insert(
                jdbc,
                "INSERT INTO staff_assignment(shift_id,staff_id,target_type,target_id,role,assignment_kind,starts_at,ends_at,status) SELECT ?,staff_id,target_type,target_id,role,assignment_kind,DATE_ADD(starts_at,INTERVAL 1 DAY),DATE_ADD(ends_at,INTERVAL 1 DAY),'SCHEDULED' FROM staff_assignment WHERE id=?",
                other,
                assignment);
        command("record-absence", leadToken, reason(worker.staffId())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT absent_at FROM shift_staff WHERE shift_id=? AND staff_id=?",
                        Object.class,
                        other,
                        worker.staffId()))
                .isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, otherAssignment))
                .isEqualTo("SCHEDULED");
    }

    @Test
    void FR_015_temporaryAccountLockDoesNotAddAnUnspecifiedLeadEligibilityRestriction() throws Exception {
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, replacement.staffId());
        jdbc.update(
                "UPDATE account SET status='TEMPORARILY_LOCKED',locked_until=? WHERE id=?",
                ShiftStore.sql(time.now().plusSeconds(600)),
                replacement.accountId());
        command("change-lead", headToken, reason(replacement.staffId())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT lead_staff_id FROM work_shift WHERE id=?", Long.class, shift))
                .isEqualTo(replacement.staffId());
    }

    @Test
    void FR_056_historyRejectsEmptyOrReversedPeriodAsInputError() throws Exception {
        mvc.perform(get("/api/v1/duty-basis-audit-logs")
                        .param("from", from.toString())
                        .param("to", from.toString())
                        .header("Authorization", managerToken))
                .andExpect(status().isUnprocessableEntity());
    }

    /** Giữ chính xác biên giờ để kiểm [bắt đầu-CFG, kết thúc], không sửa đồng hồ chung của A. */
    static class FrozenClock extends TestClockControl {
        private volatile Instant fixed;

        @Override
        public Instant instant() {
            return fixed == null ? super.instant() : fixed;
        }

        @Override
        public void setTo(Instant at) {
            fixed = at;
        }

        @Override
        public void advanceBy(Duration duration) {
            fixed = instant().plus(duration);
        }

        @Override
        public void reset() {
            fixed = null;
            super.reset();
        }
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    static class Time {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        FrozenClock phase8Clock() {
            return new FrozenClock();
        }
    }
}
