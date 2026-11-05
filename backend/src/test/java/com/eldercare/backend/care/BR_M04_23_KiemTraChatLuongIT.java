package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.care.api.events.QualityCheckFailedNotice;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 014 T023 (BR-M04-23, FR-059 → FR-062, FR-064, FR-066): người kiểm tra tự chọn công việc đã hoàn thành của ca, hệ thống
 * chỉ kiểm tỷ lệ CFG-M04-11; kết quả nhóm 3, Không đạt kèm công việc làm lại của 005 trong cùng lệnh. Nhắc mốc trước kết
 * ca, đóng quá hạn khi hết ca và GET danh sách làm ở lượt sau (plan Phase 9).
 */
class BR_M04_23_KiemTraChatLuongIT extends CareResultTestBase {
    TestUsers.TestUser caregiver;

    @org.springframework.beans.factory.annotation.Autowired
    com.eldercare.backend.support.fake.care.FakeQualityChecks qualityChecks;

    @org.springframework.beans.factory.annotation.Autowired
    com.eldercare.backend.support.fake.administration.FakeDuties duties;

    @org.springframework.beans.factory.annotation.Autowired
    com.eldercare.backend.care.internal.qualitycheck.service.QualityCheckResultSource realQualityChecks;

    @BeforeEach
    void qualityFixture() {
        caregiver = new TestUsers(jdbc, time).staff(Role.CAREGIVER);
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
    void FR_060_theFloorManagersOwnTaskIsNeitherSelectableNorCounted() throws Exception {
        long own = completed(baseManager.accountId(), performer);
        long other = completed(caregiver.accountId(), caregiver.staffId());
        select(List.of(own))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TASK_NOT_ELIGIBLE"));
        // Chỉ một công việc đủ điều kiện nên một mục là đủ tỷ lệ.
        select(List.of(other)).andExpect(status().isCreated());
    }

    @Test
    void FR_061_passIsStoredWithInspectorAndRoleOnceAndTheOriginalTaskIsUnchanged() throws Exception {
        long task = completed(caregiver.accountId(), caregiver.staffId());
        select(List.of(task)).andExpect(status().isCreated());
        long item = item(task);
        result(item, "{\"result\":\"PASS\"}").andExpect(status().isOk());
        assertThat(itemStatus(item)).isEqualTo("PASS");
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.inspectorRole')) FROM record_event WHERE id=?",
                        String.class,
                        item))
                .isEqualTo("FLOOR_MANAGER");
        assertThat(jdbc.queryForObject("SELECT confirmed_at FROM record_event WHERE id=?", Object.class, item))
                .isNotNull();
        assertThat(jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, task))
                .isEqualTo("COMPLETED");
        result(item, "{\"result\":\"FAIL\",\"note\":\"Đổi ý\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
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
        // 005 đọc nguồn qua bản giả @Primary của FakeM04Config; nguồn thật của 014 kiểm riêng bên dưới.
        qualityChecks.failed(
                item, task, new com.eldercare.backend.care.api.spi.QualityCheckPort.FailedCheckView(item, shift));
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
    void FR_066_onlyTheAssignedFloorManagerRecordsWhileTheyAreListedInTheShift() throws Exception {
        long task = completed(caregiver.accountId(), caregiver.staffId());
        select(List.of(task)).andExpect(status().isCreated());
        long item = item(task);
        user.set(caregiver.user());
        result(item, "{\"result\":\"PASS\"}").andExpect(status().isForbidden());
        select(List.of(completed(caregiver.accountId(), caregiver.staffId()))).andExpect(status().isForbidden());
        user.set(baseManager.user());
        result(item, "{\"result\":\"NOT_CHECKABLE\"}").andExpect(status().isUnprocessableEntity());
        result(item, "{\"result\":\"NOT_CHECKABLE\",\"note\":\"Người cao tuổi đã chuyển viện\"}")
                .andExpect(status().isOk());
        assertThat(itemStatus(item)).isEqualTo("NOT_CHECKABLE");
    }

    @Test
    void FR_066_shiftLeadRecordsWhenTheShiftHasNoFloorManagerButNotTheirOwnTask() throws Exception {
        var nurse = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF);
        duties.shiftWithoutFloorManager(shift);
        duties.shiftLead(floor, nurse.staffId());
        // Quyền theo nhiệm vụ của 002 (FR-019e): phân công Người phụ trách ca của ca và tầng.
        assignments.assign(
                nurse.staffId(),
                new com.eldercare.backend.administration.account.api.spi.AssignmentView(
                        com.eldercare.backend.administration.account.api.spi.AssignmentKind.SHIFT_LEAD,
                        floor,
                        null,
                        null,
                        shift,
                        time.now().minusSeconds(3600),
                        time.now().plusSeconds(3600)));
        user.set(nurse.user());
        long own = completed(nurse.accountId(), nurse.staffId());
        long other = completed(caregiver.accountId(), caregiver.staffId());
        select(List.of(own))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TASK_NOT_ELIGIBLE"));
        select(List.of(other)).andExpect(status().isCreated());
        long item = item(other);
        result(item, "{\"result\":\"PASS\"}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.inspectorRole')) FROM record_event WHERE id=?",
                        String.class,
                        item))
                .isEqualTo("SHIFT_LEAD");
    }
}
