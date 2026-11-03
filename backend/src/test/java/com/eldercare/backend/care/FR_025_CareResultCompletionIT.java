package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.care.api.VitalSignTaskApi;
import com.eldercare.backend.care.api.spi.VitalSignPort.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.care.FakeVitalSigns;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Phần còn lại T033/T036: phát sinh, quyền theo căn cứ và callback đo lại. */
class FR_025_CareResultCompletionIT extends CareResultTestBase {
    @Autowired
    ApplicationContext context;

    @Autowired
    FakeVitalSigns vitalSigns;

    @Autowired
    FakeDuties duties;

    @Autowired
    AuditLogApi audit;

    Instant performed;

    void adHocType() {
        performed = time.now();
        shifts.onShift(performer, performed, true);
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE',?)",
                type,
                "Loại phát sinh kiểm thử " + type,
                "{\"group\":\"DRINKING_ASSISTANCE\",\"professional\":false,\"resultType\":\"ML\",\"defaultRole\":\"FLOOR_MANAGER\",\"defaultImportance\":\"NORMAL\",\"upperLimit\":500,\"billable\":false,\"certificates\":[]}");
    }

    ResultActions adHoc(boolean reviewed) throws Exception {
        return mvc.perform(post("/api/v1/ad-hoc-care-recordings")
                .header("Idempotency-Key", "ad-hoc-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"residentId\":" + resident + ",\"taskTypeId\":" + type + ",\"performedAt\":\"" + performed
                        + "\",\"numericValue\":200,\"sameTypeTasksReviewed\":" + reviewed + "}"));
    }

    @Test
    void FR_025_sameTypeOpenTasksAreShownBeforeCreating() throws Exception {
        adHocType();
        adHoc(false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sameTypeTasks[0].id").value(task));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE resident_id=?", Long.class, resident))
                .isEqualTo(1);
    }

    @Test
    void FR_025_reviewedAdHocRecordingIsImmediatelyCompletedWithoutWindow() throws Exception {
        adHocType();
        adHoc(true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.currentValue.numericValue").value(200));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM care_task WHERE resident_id=? AND basis_type='AD_HOC_RECORDING' AND status='COMPLETED' AND window_from IS NULL AND window_to IS NULL AND result_performed_at IS NOT NULL",
                        Long.class,
                        resident))
                .isEqualTo(1);
    }

    @Test
    void FR_025_adHocRecordingRequiresTheRecordersShift() throws Exception {
        adHocType();
        shifts.onShift(performer, performed, false);
        adHoc(true).andExpect(status().isForbidden());
    }

    @Test
    void BR_M04_12_assignedCaregiverCanRecordTheirTask() throws Exception {
        var actor = new TestUsers(jdbc, time).staff(Role.CAREGIVER);
        user.set(actor.user());
        assignments.assign(
                actor.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floor,
                        null,
                        resident,
                        shift,
                        time.now().minusSeconds(3600),
                        time.now().plusSeconds(3600)));
        jdbc.update("UPDATE care_task SET performer_id=? WHERE id=?", actor.staffId(), task);
        record("{\"numericValue\":200}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.permissionBasis").value("ASSIGNED"));
    }

    @Test
    void BR_M04_12_unassignedCaregiverCannotRecordAnotherTask() throws Exception {
        var actor = new TestUsers(jdbc, time).staff(Role.CAREGIVER);
        user.set(actor.user());
        assignments.assign(
                actor.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floor,
                        null,
                        resident,
                        shift,
                        time.now().minusSeconds(3600),
                        time.now().plusSeconds(3600)));
        record("{\"numericValue\":200}").andExpect(status().isForbidden());
        assertThat(storedStatus()).isEqualTo("DUE");
    }

    @Test
    void BR_M04_12_shiftLeadCannotSubstituteUntilTaskIsOverdue() throws Exception {
        var actor = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF);
        user.set(actor.user());
        assignments.assign(
                actor.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_LEAD,
                        floor,
                        null,
                        null,
                        shift,
                        time.now().minusSeconds(3600),
                        time.now().plusSeconds(3600)));
        duties.shiftLead(floor, actor.staffId());
        record("{\"numericValue\":200}").andExpect(status().isForbidden());
    }

    @Test
    void BR_M04_12_shiftLeadCanRecordOverdueWithDutyBasis() throws Exception {
        var actor = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF);
        user.set(actor.user());
        assignments.assign(
                actor.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_LEAD,
                        floor,
                        null,
                        null,
                        shift,
                        time.now().minusSeconds(3600),
                        time.now().plusSeconds(3600)));
        duties.shiftLead(floor, actor.staffId());
        jdbc.update(
                "UPDATE care_task SET status='OVERDUE',window_to=? WHERE id=?",
                local(time.now().minusSeconds(1)),
                task);
        record("{\"numericValue\":200}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.permissionBasis").value("SHIFT_LEAD_DUTY"));
    }

    @Test
    void FR_041_confirmationCompletesTheSamePendingMeasurement() throws Exception {
        Instant measured = time.now();
        long vitalId = TestUsers.nextId();
        data("MEASURED_VALUE", false);
        jdbc.update("UPDATE care_task SET data=JSON_SET(data,'$.vitalSignType','TEMPERATURE') WHERE id=?", task);
        vitalSigns.result(
                new FakeVitalSigns.Measurement(
                        resident, "TEMPERATURE", new BigDecimal("37"), measured, performer, task, Optional.empty()),
                new RecordingResult(
                        RecordingStatus.AWAITING_REMEASUREMENT,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(TestUsers.nextId())));
        record("{\"numericValue\":37,\"performedAt\":\"" + measured + "\"}").andExpect(status().isAccepted());
        context.getBean("careVitalSignTasks", VitalSignTaskApi.class).confirmed(task, vitalId, measured, performer);
        assertThat(storedStatus()).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT vital_sign_record_id FROM care_task WHERE id=?", Long.class, task))
                .isEqualTo(vitalId);
    }

    @Test
    void FR_047_auditRecordsTheActualResultValues() throws Exception {
        record("{\"numericValue\":200}").andExpect(status().isOk());
        assertThat(audit.recorded(
                        new AuditAction("CARE_RESULT_RECORDED", false), "M04.CARE_RESULT", Long.toString(task), null))
                .singleElement();
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(data_after,'$.values.numericValue')) FROM audit_log WHERE entity_type='M04.CARE_RESULT' AND entity_id=? AND action='CARE_RESULT_RECORDED'",
                        String.class,
                        Long.toString(task)))
                .isEqualTo("200");
    }
}
