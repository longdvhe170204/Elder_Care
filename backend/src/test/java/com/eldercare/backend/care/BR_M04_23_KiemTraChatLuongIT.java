package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.care.api.events.QualityCheckFailedNotice;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T023 (BR-M04-23, FR-059 → FR-062, FR-064, FR-066): người kiểm tra tự chọn công việc đã hoàn thành của ca, hệ thống
 * chỉ kiểm tỷ lệ CFG-M04-11; kết quả nhóm 3, Không đạt kèm công việc làm lại của 005 trong cùng lệnh. Nhắc mốc trước kết
 * ca, đóng quá hạn khi hết ca và GET danh sách làm ở lượt sau (plan Phase 9).
 */
class BR_M04_23_KiemTraChatLuongIT extends CareResultTestBase {
    TestUsers.TestUser caregiver;
    Instant shiftEndsAt;

    @org.springframework.beans.factory.annotation.Autowired
    com.eldercare.backend.support.fake.administration.FakeDuties duties;

    @org.springframework.beans.factory.annotation.Autowired
    com.eldercare.backend.care.internal.qualitycheck.service.QualityCheckClockStep clock;

    @org.springframework.beans.factory.annotation.Autowired
    com.eldercare.backend.care.internal.qualitycheck.service.QualityCheckResultSource realQualityChecks;

    @org.springframework.beans.factory.annotation.Autowired
    TestClockControl testClock;

    @BeforeEach
    void qualityFixture() {
        caregiver = new TestUsers(jdbc, time).staff(Role.CAREGIVER);
        shiftEndsAt = shifts.shift(shift).orElseThrow().endsAt();
        duties.floorManager(floor, baseManager.staffId());
        user.set(baseManager.user());
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE',?)",
                type,
                "Vệ sinh cá nhân " + type,
                "{\"group\":\"OTHER\",\"professional\":false,\"resultType\":\"ML\",\"defaultRole\":\"CAREGIVER\",\"defaultImportance\":\"NORMAL\",\"billable\":false,\"certificates\":[]}");
    }

    /** Công việc đã hoàn thành trong ca, do tài khoản {@code recorder} ghi kết quả. */
    long completed(long recorderAccount, long performerStaff) {
        long id = TestUsers.nextId();
        Instant done = time.now().minusSeconds(600);
        jdbc.update(
                "INSERT INTO care_task(id,resident_id,task_type_id,basis_type,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,performer_id,status,data,created_by,created_at,"
                        + "result_type,result_values,result_performed_at,result_recorded_at,result_recorded_by,result_permission_basis)"
                        + " VALUES(?,?,?,'AD_HOC',?,?,?,?,?,'NORMAL','CAREGIVER',?,'COMPLETED',?,?,?,'ML','{\"ml\":200}',?,?,?,'ASSIGNED')",
                id,
                resident,
                type,
                shift,
                floor,
                local(done),
                local(done.minusSeconds(900)),
                local(done.plusSeconds(900)),
                performerStaff,
                "{\"publicTaskTypeId\":" + type
                        + ",\"taskTypeName\":\"Vệ sinh cá nhân\",\"content\":\"Kiểm thử\",\"resultType\":\"ML\",\"billable\":false,\"upperLimit\":500,\"certificates\":[]}",
                baseManager.accountId(),
                local(done.minusSeconds(1800)),
                local(done),
                local(done),
                recorderAccount);
        return id;
    }

    List<Long> completed(int count) {
        var ids = new ArrayList<Long>();
        for (int i = 0; i < count; i++) ids.add(completed(caregiver.accountId(), caregiver.staffId()));
        return ids;
    }

    ResultActions select(List<Long> tasks) throws Exception {
        return mvc.perform(post("/api/v1/quality-checks/shifts/" + shift)
                .header("Idempotency-Key", "qc-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"floorAreaId\":" + floor + ",\"careTaskIds\":" + tasks + "}"));
    }

    ResultActions result(long item, String body) throws Exception {
        return mvc.perform(post("/api/v1/quality-check-items/" + item + "/result")
                .header("Idempotency-Key", "qcr-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    long item(long task) {
        return jdbc.queryForObject(
                "SELECT id FROM record_event WHERE kind='M04.QUALITY_CHECK_ITEM' AND unique_key=?",
                Long.class,
                "QC_TASK:" + task);
    }

    String itemStatus(long item) {
        return jdbc.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.status')) FROM record_event WHERE id=?",
                String.class,
                item);
    }

    @Test
    void BR_M04_23_selectionBelowTheRatioIsBlockedThenTheListIsCreatedAndItemsCanBeAddedOnce() throws Exception {
        var tasks = completed(21); // 5% × 21 làm tròn lên = 2
        select(tasks.subList(0, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SAMPLE_RATIO_NOT_MET"));
        select(tasks.subList(0, 2)).andExpect(status().isCreated());
        assertThat(itemStatus(item(tasks.get(0)))).isEqualTo("PENDING");
        // Thêm mục tới hết ca, không xét lại tỷ lệ; mỗi công việc chỉ được chọn một lần.
        select(tasks.subList(2, 3)).andExpect(status().isCreated());
        select(tasks.subList(2, 3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TASK_NOT_ELIGIBLE"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE kind='M04.QUALITY_CHECKLIST' AND unique_key=?",
                        Integer.class,
                        shift + ":" + floor))
                .isEqualTo(1);
        // Người thực hiện không được báo khi công việc được chọn.
        assertThat(events.stream(QualityCheckFailedNotice.class)).isEmpty();
    }

    @Test
    void FR_064_failNeedsANoteAndAReworkTaskCreatedInTheSameCommandAndThePerformerIsNotified() throws Exception {
        long task = completed(caregiver.accountId(), caregiver.staffId());
        select(List.of(task)).andExpect(status().isCreated());
        long item = item(task);
        result(item, "{\"result\":\"FAIL\"}").andExpect(status().isUnprocessableEntity());
        result(item, "{\"result\":\"FAIL\",\"note\":\"Chưa vệ sinh răng miệng\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REWORK_TASK_REQUIRED"));
        assertThat(itemStatus(item)).isEqualTo("PENDING");
        assertThat(realQualityChecks.failed(item, task)).isEmpty();
        result(
                        item,
                        "{\"result\":\"FAIL\",\"note\":\"Chưa vệ sinh răng miệng\",\"reworkTask\":{\"plannedAt\":\""
                                + time.now().plusSeconds(600) + "\",\"sharedOnFloor\":true}}")
                .andExpect(status().isOk());
        assertThat(itemStatus(item)).isEqualTo("FAIL");
        assertThat(realQualityChecks.failed(item, task))
                .hasValueSatisfying(v -> assertThat(v.shiftId()).isEqualTo(shift));
        assertThat(realQualityChecks.failed(item, TestUsers.nextId())).isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM care_task WHERE basis_type='REWORK' AND basis_id=? AND original_task_id=?",
                        Integer.class,
                        item,
                        task))
                .isEqualTo(1);
        assertThat(events.stream(QualityCheckFailedNotice.class))
                .singleElement()
                .satisfies(n -> assertThat(n.performerStaffId()).isEqualTo(caregiver.staffId()));
        assertThat(jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, task))
                .isEqualTo("COMPLETED");
    }

    @Test
    void Q_404_checksEffectiveResultsShiftWindowAndInspectorRules() throws Exception {
        com.eldercare.backend.support.TableRows.each(List.of("AMEND", "SHIFT_END", "INSPECTOR"), scenario -> {
            fixture();
            qualityFixture();
            switch (scenario) {
                case "AMEND" -> Q_404_amendsTheEffectiveResultAndKeepsTheReworkWhenAmendedAgain();
                case "SHIFT_END" -> Q_404_rejectsAmendmentAfterTheShiftEnds();
                case "INSPECTOR" -> FR_066_onlyAnEligibleInspectorCanAmendAndTheShiftLeadCannotCheckTheirOwnWork();
            }
        });
    }

    private void Q_404_amendsTheEffectiveResultAndKeepsTheReworkWhenAmendedAgain() throws Exception {
        long task = completed(caregiver.accountId(), caregiver.staffId());
        select(List.of(task)).andExpect(status().isCreated());
        long item = item(task);
        result(item, "{\"result\":\"PASS\"}").andExpect(status().isOk());

        correct(item, "AMEND_CONTENT", "{\"result\":\"FAIL\",\"note\":\"Cần làm lại\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("REWORK_TASK_REQUIRED"));
        correct(
                        item,
                        "AMEND_CONTENT",
                        "{\"result\":\"FAIL\",\"note\":\"Cần làm lại\",\"reworkTask\":{\"plannedAt\":\""
                                + time.now().plusSeconds(600) + "\",\"sharedOnFloor\":true}}")
                .andExpect(status().isCreated());
        assertThat(realQualityChecks.failed(item, task)).isPresent();
        assertThat(reworkCount(item)).isEqualTo(1);
        assertThat(itemStatus(item)).isEqualTo("PASS");

        correct(item, "AMEND_CONTENT", "{\"result\":\"NOT_CHECKABLE\",\"note\":\"Đã chuyển viện\"}")
                .andExpect(status().isCreated());
        assertThat(realQualityChecks.failed(item, task)).isEmpty();
        assertThat(reworkCount(item)).isEqualTo(1);
        mvc.perform(get("/api/v1/quality-checks/shifts/" + shift).param("floorAreaId", Long.toString(floor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("NOT_CHECKABLE"));
        correct(item, "AMEND_CONTENT", "{\"result\":\"PASS\"}").andExpect(status().isCreated());
        correct(item, "VOID", "null")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CORRECTION_NOT_SUPPORTED"));
    }

    private void Q_404_rejectsAmendmentAfterTheShiftEnds() throws Exception {
        long task = completed(caregiver.accountId(), caregiver.staffId());
        select(List.of(task)).andExpect(status().isCreated());
        long item = item(task);
        result(item, "{\"result\":\"PASS\"}").andExpect(status().isOk());
        testClock.setTo(shiftEndsAt);
        try {
            correct(item, "AMEND_CONTENT", "{\"result\":\"NOT_CHECKABLE\",\"note\":\"Không còn dấu vết\"}")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.errorCode").value("OUTSIDE_SHIFT_TIME"));
        } finally {
            testClock.reset();
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void Q_404_rejectsAnInvalidReworkAndRollsBackTheCorrection() throws Exception {
        long task = completed(caregiver.accountId(), caregiver.staffId());
        select(List.of(task)).andExpect(status().isCreated());
        long item = item(task);
        result(item, "{\"result\":\"PASS\"}").andExpect(status().isOk());
        correct(
                        item,
                        "AMEND_CONTENT",
                        "{\"result\":\"FAIL\",\"note\":\"Cần làm lại\",\"reworkTask\":{\"plannedAt\":\"" + shiftEndsAt
                                + "\",\"sharedOnFloor\":true}}")
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/v1/corrections/M04.QUALITY_CHECK_ITEM/" + item))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentContent.result").value("PASS"))
                .andExpect(jsonPath("$.corrections").isEmpty());
        assertThat(reworkCount(item)).isZero();
    }

    private void FR_066_onlyAnEligibleInspectorCanAmendAndTheShiftLeadCannotCheckTheirOwnWork() throws Exception {
        var lead = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF);
        long leadTask = completed(lead.accountId(), lead.staffId());
        long otherTask = completed(caregiver.accountId(), caregiver.staffId());
        select(List.of(leadTask, otherTask)).andExpect(status().isCreated());
        long ownItem = item(leadTask);
        long otherItem = item(otherTask);
        result(ownItem, "{\"result\":\"PASS\"}").andExpect(status().isOk());
        result(otherItem, "{\"result\":\"PASS\"}").andExpect(status().isOk());

        user.set(caregiver.user());
        mvc.perform(get("/api/v1/corrections/M04.QUALITY_CHECK_ITEM/" + otherItem))
                .andExpect(status().isForbidden());
        duties.shiftLead(floor, lead.staffId());
        assignments.assign(
                lead.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_LEAD,
                        floor,
                        null,
                        null,
                        shift,
                        time.now().minusSeconds(60),
                        shiftEndsAt));
        user.set(lead.user());
        String amendment = "{\"result\":\"NOT_CHECKABLE\",\"note\":\"Không còn dấu vết\"}";
        correct(otherItem, "AMEND_CONTENT", amendment).andExpect(status().isForbidden());

        duties.shiftWithoutFloorManager(shift);
        correct(ownItem, "AMEND_CONTENT", amendment)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SELF_CHECK_NOT_ALLOWED"));
        correct(otherItem, "AMEND_CONTENT", amendment).andExpect(status().isCreated());
    }

    private ResultActions correct(long item, String type, String correctedContent) throws Exception {
        return mvc.perform(post("/api/v1/corrections/M04.QUALITY_CHECK_ITEM/" + item)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"" + type + "\",\"correctedContent\":" + correctedContent
                        + ",\"reason\":\"Đối chiếu lại kết quả\"}"));
    }

    private int reworkCount(long item) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM care_task WHERE basis_type='REWORK' AND basis_id=?", Integer.class, item);
    }

    java.util.List<com.eldercare.backend.care.api.events.QualityCheckManagerNotice> managerNotices(String cause) {
        return events.stream(com.eldercare.backend.care.api.events.QualityCheckManagerNotice.class)
                .filter(n -> n.shiftId() == shift && n.cause().equals(cause))
                .toList();
    }
}
