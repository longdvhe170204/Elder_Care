package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.administration.*;
import com.eldercare.backend.support.fake.care.*;
import com.eldercare.backend.support.fake.resident.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** T019: HTTP thật/MySQL, nguồn fixture duy nhất do chủ interface công bố. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@org.springframework.test.context.event.RecordApplicationEvents
@Import({FR_028a_ReasonCatalogHttpIT.Users.class, FakeM15Config.class, FakeM01Config.class, FakeM04Config.class})
class BR_M04_01_ShiftTaskPlanningIT extends MySqlIntegrationTestBase {
    @Autowired
    org.springframework.context.ApplicationContext context;

    @Autowired
    org.springframework.test.context.event.ApplicationEvents recorded;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    com.eldercare.backend.common.time.TestClockControl clock;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakeResidents residents;

    @Autowired
    FakeContracts contracts;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeBedAllocations beds;

    @Autowired
    FakeShiftStaff shifts;

    @Autowired
    @Qualifier("fakeMealTaskSource")
    FakeTaskSource meals;

    @Autowired
    JsonMapper json;

    long residentId, floor, shiftId, typeId, sourceId;
    Instant planned;
    TestUsers.TestUser manager, caregiver;

    @BeforeEach
    void fixture() {
        clock.setTo(time.today().atTime(8, 0).atZone(BusinessTime.ZONE).toInstant());
        residentId = TestUsers.nextId();
        floor = TestUsers.nextId();
        shiftId = TestUsers.nextId();
        typeId = TestUsers.nextId();
        sourceId = TestUsers.nextId();
        planned = time.now().plusSeconds(1800);
        var users = new TestUsers(jdbc, time);
        manager = users.staff(Role.FLOOR_MANAGER);
        caregiver = users.staff(Role.CAREGIVER);
        user.set(manager.user());
        assignments.assign(
                manager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(86400),
                        null));
        residents.resident(residentId, "[Họ tên kiểm thử]", Gender.MALE, ResidentStatus.IN_RESIDENCE);
        contracts.period(
                residentId,
                new ResidenceTypePeriod(
                        ResidenceType.LONG_TERM_RESIDENTIAL, time.today().minusDays(1), Optional.empty()));
        beds.allocate(
                residentId, new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floor));
        shifts.shift(new ShiftView(
                shiftId, floor, time.now().minusSeconds(3600), time.now().plusSeconds(14400), true));
        shifts.assignments(
                residentId, shiftId, List.of(new CareAssignmentView(caregiver.staffId(), Role.CAREGIVER, true)));
        shifts.members(shiftId, List.of(new ShiftMemberView(caregiver.staffId(), Role.CAREGIVER)));
        shifts.onShift(caregiver.staffId(), planned, true);
        shifts.qualified(
                caregiver.staffId(), Set.of(), planned.atZone(BusinessTime.ZONE).toLocalDate(), true);
        meals.validation(SourceType.MEAL, sourceId, residentId, planned, new SourceValidation(true, true, true, false));
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE',?)",
                typeId,
                "Loại kiểm thử " + typeId,
                "{\"group\":\"OTHER\",\"professional\":false,\"resultType\":\"PERFORMED\",\"defaultRole\":\"CAREGIVER\",\"defaultImportance\":\"NORMAL\",\"billable\":false,\"certificates\":[]}");
    }

    @AfterEach
    void restoreClock() {
        clock.reset();
    }

    String row(long resident, long source, Instant at) {
        return "{\"residentId\":" + resident + ",\"taskTypeId\":" + typeId + ",\"basis\":{\"type\":\"MEAL\",\"id\":"
                + source + "},\"content\":\"Nội dung kiểm thử\",\"plannedAt\":\"" + at
                + "\",\"windowBeforeMinutes\":15,\"windowAfterMinutes\":15,\"importance\":\"NORMAL\",\"performerId\":"
                + caregiver.staffId() + ",\"shared\":false}";
    }

    ResultActions plan(String body) throws Exception {
        return mvc.perform(post("/api/v1/shifts/" + shiftId + "/care-tasks/plan")
                .header("Idempotency-Key", "task-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    long create() throws Exception {
        var result = plan("[" + row(residentId, sourceId, planned) + "]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].status").value("NOT_YET_DUE"))
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asLong();
    }

    @Test
    void BR_M04_01_validBatchPersistsAndCanBeRead() throws Exception {
        long id = create();
        mvc.perform(get("/api/v1/care-tasks/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residentId").value(residentId))
                .andExpect(jsonPath("$.basis.id").value(sourceId));
        mvc.perform(get("/api/v1/shifts/" + shiftId + "/care-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M04.TASK' AND entity_id=?",
                        Integer.class,
                        Long.toString(id)))
                .isPositive();
    }

    @Test
    void BR_M04_01_invalidSecondRowRejectsTheWholeBatchWithOneBasedErrors() throws Exception {
        plan("[" + row(residentId, sourceId, planned) + "," + row(residentId, sourceId + 1, planned) + "]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rowErrors[0].row").value(2))
                .andExpect(jsonPath("$.rowErrors[0].codes[0]").value("INVALID_BASIS"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE shift_id=?", Integer.class, shiftId))
                .isZero();
    }

    @Test
    void DBR_12_duplicateBasisRejectedAndCancelledTaskCanBeReplanned() throws Exception {
        long id = create();
        plan("[" + row(residentId, sourceId, planned) + "]").andExpect(status().isConflict());
        mvc.perform(post("/api/v1/care-tasks/" + id + "/cancel")
                        .header("Idempotency-Key", "cancel-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Điều chỉnh công việc\"}"))
                .andExpect(status().isOk());
        plan("[" + row(residentId, sourceId, planned) + "]").andExpect(status().isCreated());
    }

    @Test
    void DBR_12_duplicateRowsWithinBatchAreRejectedAtomically() throws Exception {
        var r = row(residentId, sourceId, planned);
        plan("[" + r + "," + r + "]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rowErrors[0].codes").value(org.hamcrest.Matchers.hasItem("DUPLICATE_BASIS")));
    }

    @Test
    void FR_017_absentResidentCannotBePlannedEvenForFutureReturn() throws Exception {
        residents.resident(residentId, "[Họ tên kiểm thử]", Gender.MALE, ResidentStatus.TEMPORARILY_ABSENT);
        plan("[" + row(residentId, sourceId, planned) + "]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rowErrors[0].codes").value(org.hamcrest.Matchers.hasItem("RESIDENT_ABSENT")));
    }

    @Test
    void FR_018_dayCareWithoutArrivalIsBlocked() throws Exception {
        long dayCare = TestUsers.nextId();
        residents.resident(dayCare, "[Họ tên kiểm thử]", Gender.MALE, ResidentStatus.IN_RESIDENCE);
        contracts.period(dayCare, new ResidenceTypePeriod(ResidenceType.DAY_CARE, time.today(), Optional.empty()));
        beds.allocate(
                dayCare, new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floor));
        plan("[" + row(dayCare, sourceId, planned) + "]")
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.rowErrors[0].codes").value(org.hamcrest.Matchers.hasItem("DAY_CARE_NOT_PRESENT")));
    }

    @Test
    void BR_M09_01_missingCertificateIsBlocked() throws Exception {
        jdbc.update(
                "UPDATE catalog_item SET attributes=JSON_SET(attributes,'$.certificates',JSON_ARRAY('CERT_TEST')) WHERE id=?",
                typeId);
        plan("[" + row(residentId, sourceId, planned) + "]")
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.rowErrors[0].codes").value(org.hamcrest.Matchers.hasItem("MISSING_CERTIFICATE")));
    }

    @Test
    void FR_017_caregiverCannotPlanTheShift() throws Exception {
        user.set(caregiver.user());
        plan("[" + row(residentId, sourceId, planned) + "]").andExpect(status().isForbidden());
    }

    @Test
    void BR_M04_26_lateCreationStartsDueOrOverdue() throws Exception {
        for (int ago : new int[] {60, 1800}) {
            Instant at = time.now().minusSeconds(ago);
            long source = TestUsers.nextId();
            shifts.onShift(caregiver.staffId(), at, true);
            meals.validation(SourceType.MEAL, source, residentId, at, new SourceValidation(true, true, true, false));
            plan("[" + row(residentId, source, at) + "]")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].status").value(ago == 60 ? "DUE" : "OVERDUE"));
        }
    }

    @Test
    void FR_017_sourceDataReturnsActualIdsWithoutSuggestedTasks() throws Exception {
        meals.sources(residentId, List.of(new SourceView(SourceType.MEAL, sourceId, planned, "Bữa kiểm thử")));
        mvc.perform(get("/api/v1/shifts/" + shiftId + "/source-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shift.id").value(shiftId))
                .andExpect(jsonPath("$.residents[0].otherSources[0].id").value(sourceId))
                .andExpect(jsonPath("$.suggestedTasks").doesNotExist());
    }

    @Test
    void FR_022_sourceCancellationOnlyCancelsTasksThatAreNotYetDue() throws Exception {
        long id = create();
        var api = context.getBeanProvider(com.eldercare.backend.care.api.TaskSourceApi.class)
                .getIfAvailable();
        assertThat(api).as("Nguồn thật callback T025").isNotNull();
        api.sourceCancelled(SourceType.MEAL, sourceId, new com.eldercare.backend.common.Reason("Bữa đã hủy"));
        assertThat(jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, id))
                .isEqualTo("CANCELLED");
        Instant at = time.now().minusSeconds(60);
        long source = TestUsers.nextId();
        shifts.onShift(caregiver.staffId(), at, true);
        meals.validation(SourceType.MEAL, source, residentId, at, new SourceValidation(true, true, true, false));
        plan("[" + row(residentId, source, at) + "]").andExpect(status().isCreated());
        api.sourceCancelled(SourceType.MEAL, source, new com.eldercare.backend.common.Reason("Bữa đã hủy"));
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM care_task WHERE basis_id=? AND shift_id=?", String.class, source, shiftId))
                .isEqualTo("DUE");
    }

    @Test
    void FR_022_sourceChangeOnlyNotifiesWithoutCancellingOrCreatingTasks() throws Exception {
        long id = create();
        var api = context.getBeanProvider(com.eldercare.backend.care.api.TaskSourceApi.class)
                .getIfAvailable();
        assertThat(api).isNotNull();
        api.sourceChanged(SourceType.MEAL, sourceId);
        assertThat(jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, id))
                .isEqualTo("NOT_YET_DUE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE shift_id=?", Integer.class, shiftId))
                .isEqualTo(1);
        assertThat(recorded.stream(com.eldercare.backend.care.api.events.TaskSourceChanged.class)
                        .filter(e -> e.sourceId() == sourceId)
                        .count())
                .isEqualTo(1);
    }

    @Test
    void BR_M04_24_shiftStartCheckIsUniquePerNurseAndUsesTheEntireShift() throws Exception {
        var nurse = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF);
        jdbc.update(
                "UPDATE catalog_item SET attributes=JSON_SET(attributes,'$.group','SHIFT_START_CHECK','$.defaultRole','NURSING_STAFF','$.professional',CAST('true' AS JSON)) WHERE id=?",
                typeId);
        shifts.members(shiftId, List.of(new ShiftMemberView(nurse.staffId(), Role.NURSING_STAFF)));
        shifts.onShift(nurse.staffId(), planned, true);
        shifts.qualified(nurse.staffId(), Set.of(), time.today(), true);
        String r = "{\"location\":{\"type\":\"FLOOR\",\"id\":" + floor + "},\"taskTypeId\":" + typeId
                + ",\"basis\":{\"type\":\"SHIFT_START_CHECK\"},\"plannedAt\":\"" + planned
                + "\",\"importance\":\"NORMAL\",\"performerId\":" + nurse.staffId() + "}";
        plan("[" + r + "]").andExpect(status().isCreated());
        plan("[" + r + "]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rowErrors[0].codes")
                        .value(org.hamcrest.Matchers.hasItem("SHIFT_CHECK_ALREADY_EXISTS")));
        assertThat(jdbc.queryForObject(
                        "SELECT TIMESTAMPDIFF(SECOND,window_from,window_to) FROM care_task WHERE shift_id=?",
                        Long.class,
                        shiftId))
                .isEqualTo(18000);
    }

    @Test
    void FR_026_closedTaskCannotBeMutatedEvenByDirectSql() throws Exception {
        long id = create();
        jdbc.update("UPDATE care_task SET status='CANCELLED' WHERE id=?", id);
        assertThatThrownBy(() -> jdbc.update("UPDATE care_task SET status='DUE' WHERE id=?", id))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    void BR_M04_26_clockStepMovesDueTasksWithoutCreatingNewOnes() throws Exception {
        long id = create();
        var step =
                context
                        .getBeanProvider(com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork.class)
                        .stream()
                        .filter(w -> w.getClass().getName().contains("CareTaskDueStep"))
                        .findFirst();
        assertThat(step).as("Bước chuyển theo đồng hồ của T024").isPresent();
        var from = jdbc.queryForObject("SELECT window_from FROM care_task WHERE id=?", LocalDateTime.class, id)
                .atZone(BusinessTime.ZONE)
                .toInstant();
        var to = jdbc.queryForObject("SELECT window_to FROM care_task WHERE id=?", LocalDateTime.class, id)
                .atZone(BusinessTime.ZONE)
                .toInstant();
        step.orElseThrow().run(from);
        assertThat(jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, id))
                .isEqualTo("DUE");
        step.orElseThrow().run(to.minusNanos(1000));
        assertThat(jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, id))
                .isEqualTo("DUE");
        // T044/FR-043: từ Phase7, bước theo phút xử lý cả mốc hết khung.
        step.orElseThrow().run(to);
        assertThat(jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, id))
                .isEqualTo("OVERDUE");
        step.orElseThrow().run(to);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE shift_id=?", Integer.class, shiftId))
                .isEqualTo(1);
    }

    @Test
    void BR_M04_26_lateCreationImmediatelyRemindsWithoutDuplicatingOnRejectedRetry() throws Exception {
        Instant at = time.now().minusSeconds(1800);
        shifts.onShift(caregiver.staffId(), at, true);
        meals.validation(SourceType.MEAL, sourceId, residentId, at, new SourceValidation(true, true, true, false));
        plan("[" + row(residentId, sourceId, at) + "]").andExpect(status().isCreated());
        long id = jdbc.queryForObject("SELECT id FROM care_task WHERE shift_id=?", Long.class, shiftId);
        assertThat(recorded.stream(com.eldercare.backend.care.api.events.OverdueTaskReminder.class)
                        .filter(e -> e.taskId() == id)
                        .count())
                .isEqualTo(1);
        plan("[" + row(residentId, sourceId, at) + "]").andExpect(status().isConflict());
        assertThat(recorded.stream(com.eldercare.backend.care.api.events.OverdueTaskReminder.class)
                        .filter(e -> e.taskId() == id)
                        .count())
                .isEqualTo(1);
    }

    @Test
    void BR_M04_26_importantLateTaskNotifiesTheFloorManager() throws Exception {
        Instant at = time.now().minusSeconds(1800);
        shifts.onShift(caregiver.staffId(), at, true);
        meals.validation(SourceType.MEAL, sourceId, residentId, at, new SourceValidation(true, true, true, false));
        plan("[" + row(residentId, sourceId, at).replace("\"importance\":\"NORMAL\"", "\"importance\":\"IMPORTANT\"")
                        + "]")
                .andExpect(status().isCreated());
        assertThat(recorded.stream(com.eldercare.backend.care.api.events.OverdueTaskNeedsHandling.class)
                        .filter(e -> e.context().floorAreaId().orElse(-1L) == floor)
                        .count())
                .isEqualTo(1);
    }

    @Test
    void BR_M04_26_mandatoryLateTaskRequestsAMediumAlert() throws Exception {
        Instant at = time.now().minusSeconds(1800);
        shifts.onShift(caregiver.staffId(), at, true);
        meals.validation(SourceType.MEAL, sourceId, residentId, at, new SourceValidation(true, true, true, false));
        plan("[" + row(residentId, sourceId, at).replace("\"importance\":\"NORMAL\"", "\"importance\":\"MANDATORY\"")
                        + "]")
                .andExpect(status().isCreated());
        var alerts = recorded.stream(com.eldercare.backend.care.api.events.AlertRequested.class)
                .filter(e -> e.residentId() == residentId)
                .toList();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.getFirst().level())
                .isEqualTo(com.eldercare.backend.common.notification.NotificationLevel.MEDIUM);
    }

    @Test
    void FR_017_missingTaskTypeIsReportedOnItsRowWithoutPersistingTheBatch() throws Exception {
        var invalid = row(residentId, sourceId, planned)
                .replace("\"taskTypeId\":" + typeId, "\"taskTypeId\":" + TestUsers.nextId());
        plan("[" + row(residentId, sourceId, planned) + "," + invalid + "]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rowErrors[0].row").value(2));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE shift_id=?", Integer.class, shiftId))
                .isZero();
    }

    @Test
    void FR_027_cannotCancelWhenWindowHasOpenedEvenIfTheJobIsDelayed() throws Exception {
        long id = create();
        jdbc.update(
                "UPDATE care_task SET window_from=? WHERE id=?",
                LocalDateTime.ofInstant(time.now().minusSeconds(60), BusinessTime.ZONE),
                id);
        mvc.perform(post("/api/v1/care-tasks/" + id + "/cancel")
                        .header("Idempotency-Key", "cancel-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Yêu cầu hủy\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void FR_028_detailRetainsTheCancellationReasonAndRule() throws Exception {
        long id = create();
        mvc.perform(post("/api/v1/care-tasks/" + id + "/cancel")
                        .header("Idempotency-Key", "cancel-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Điều chỉnh công việc\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/care-tasks/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reason").value("Điều chỉnh công việc"))
                .andExpect(jsonPath("$.closureRule").value("MANUAL_CANCEL"));
    }

    @Test
    void DBR_12_databaseUniqueRejectsAWriterThatBypassesTheValidator() throws Exception {
        long id = create();
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO care_task(resident_id,task_type_id,basis_type,basis_id,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,performer_id,status,data,created_by,created_at) SELECT resident_id,task_type_id,basis_type,basis_id,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,performer_id,status,data,created_by,created_at FROM care_task WHERE id=?",
                        id))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    }

    @Test
    void FR_025_adHocCreationUsesTheSamePerformerChecks() throws Exception {
        String body = row(residentId, sourceId, planned).replace("\"MEAL\"", "\"AD_HOC\"");
        mvc.perform(post("/api/v1/care-tasks/ad-hoc")
                        .header("Idempotency-Key", "adhoc-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void FR_026a_reworkUsesTheFailedCheckShiftAndPreservesTheOriginal() throws Exception {
        long original = create();
        // 014 T028: căn cứ làm lại là mục kiểm tra Không đạt thật của 014 (QualityCheckResultSource), không qua bản
        // giả.
        long checkId = failedCheck(original);
        Instant next = planned.plusSeconds(60);
        shifts.onShift(caregiver.staffId(), next, true);
        mvc.perform(post("/api/v1/care-tasks/" + original + "/rework")
                        .header("Idempotency-Key", "rework-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qualityCheckResultId\":" + checkId + ",\"plannedAt\":\"" + next
                                + "\",\"performerId\":" + caregiver.staffId() + "}"))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject(
                        "SELECT original_task_id FROM care_task WHERE basis_type='REWORK' AND basis_id=?",
                        Long.class,
                        checkId))
                .isEqualTo(original);
        assertThat(jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, original))
                .isEqualTo("NOT_YET_DUE");
    }

    private long failedCheck(long task) {
        long checklist = TestUsers.nextId();
        return context.getBean(com.eldercare.backend.administration.shared.api.RecordEventApi.class)
                .add(
                        com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds.ITEMS,
                        new com.eldercare.backend.administration.shared.api.dto.EntityRef(
                                "M04.QUALITY_CHECKLIST", checklist),
                        new com.eldercare.backend.administration.shared.api.dto.NewRecordEvent<>(
                                new com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds.Item(
                                        checklist,
                                        shiftId,
                                        floor,
                                        task,
                                        null,
                                        null,
                                        com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds.FAIL,
                                        "Chưa đạt",
                                        List.of(),
                                        user.accountId(),
                                        "FLOOR_MANAGER",
                                        time.now()),
                                null,
                                user.accountId(),
                                false,
                                time.now(),
                                null,
                                time.now(),
                                floor,
                                "QC_TASK:" + task))
                .id();
    }

    private long effectiveItem() {
        long planId = TestUsers.nextId(), itemId = TestUsers.nextId();
        var data = new com.eldercare.backend.care.internal.careplan.domain.CarePlanItemData(
                typeId,
                "Mục kiểm thử",
                "TIMES_PER_DAY",
                null,
                1,
                null,
                List.of(planned.atZone(BusinessTime.ZONE).toLocalTime()),
                List.of(),
                15,
                15,
                Role.CAREGIVER,
                com.eldercare.backend.care.api.dto.ResultType.PERFORMED,
                com.eldercare.backend.care.api.dto.Importance.NORMAL,
                false,
                null);
        jdbc.update(
                "INSERT INTO care_plan(id,resident_id,revision,status,effective_from,created_by,created_at) VALUES(?,?,1,'DRAFT',?,?,?)",
                planId,
                residentId,
                time.today(),
                manager.accountId(),
                LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE));
        jdbc.update(
                "INSERT INTO care_plan_item(id,care_plan_id,kind,task_type_id,frequency_type,data) VALUES(?,?,'TASK',?,'TIMES_PER_DAY',?)",
                itemId,
                planId,
                typeId,
                json.writeValueAsString(data));
        jdbc.update("UPDATE care_plan SET status='EFFECTIVE' WHERE id=?", planId);
        return itemId;
    }

    @Test
    void FR_017_effectivePlanItemMustMatchItsTimeWindowAndImportance() throws Exception {
        long item = effectiveItem();
        String r = row(residentId, sourceId, planned)
                .replace("\"MEAL\"", "\"CARE_PLAN_ITEM\"")
                .replace("\"id\":" + sourceId, "\"id\":" + item);
        plan("[" + r.replace("\"importance\":\"NORMAL\"", "\"importance\":\"IMPORTANT\"") + "]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rowErrors[0].codes").value(org.hamcrest.Matchers.hasItem("PLAN_ITEM_MISMATCH")));
        plan("[" + r + "]").andExpect(status().isCreated());
    }

    @Test
    void FR_006_oldRevisionDueWindowBlocksTheNewRevision() throws Exception {
        Instant old = time.now().minusSeconds(60);
        shifts.onShift(caregiver.staffId(), old, true);
        meals.validation(SourceType.MEAL, sourceId, residentId, old, new SourceValidation(true, true, true, false));
        plan("[" + row(residentId, sourceId, old) + "]").andExpect(status().isCreated());
        jdbc.update(
                "UPDATE care_task SET basis_type='CARE_PLAN_ITEM',plan_id=?,window_to=? WHERE shift_id=?",
                TestUsers.nextId(),
                LocalDateTime.ofInstant(planned.plusSeconds(60), BusinessTime.ZONE),
                shiftId);
        long item = effectiveItem();
        String r = row(residentId, sourceId, planned)
                .replace("\"MEAL\"", "\"CARE_PLAN_ITEM\"")
                .replace("\"id\":" + sourceId, "\"id\":" + item);
        plan("[" + r + "]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rowErrors[0].codes")
                        .value(org.hamcrest.Matchers.hasItem("OLD_REVISION_WINDOW_OVERLAP")));
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void DBR_12_concurrentTransactionsPersistExactlyOneTask() throws Exception {
        var service = context.getBean(com.eldercare.backend.care.internal.task.service.CareTaskService.class);
        var draft = new com.eldercare.backend.care.internal.task.domain.CareTaskDraft(
                residentId,
                null,
                null,
                typeId,
                SourceType.MEAL,
                sourceId,
                "Công việc kiểm thử",
                planned,
                15,
                15,
                com.eldercare.backend.care.api.dto.Importance.NORMAL,
                caregiver.staffId(),
                false);
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var call = (java.util.concurrent.Callable<String>) () -> {
                start.await();
                try {
                    service.plan(service.shiftTarget(shiftId), shiftId, List.of(draft));
                    return "CREATED";
                } catch (com.eldercare.backend.common.error.BusinessException e) {
                    return e.errorCode();
                }
            };
            var one = pool.submit(call);
            var two = pool.submit(call);
            start.countDown();
            assertThat(List.of(
                            one.get(30, java.util.concurrent.TimeUnit.SECONDS),
                            two.get(30, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CREATED", "UNMET_CONDITIONS");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE shift_id=?", Integer.class, shiftId))
                    .isEqualTo(1);
        } finally {
            // Chỉ dọn task đã commit của ca này, tránh job ở context khác đọc hồ sơ fixture không có trong bảng thật.
            try (var connection = retentionConnection();
                    var statement = connection.prepareStatement("DELETE FROM care_task WHERE shift_id=?")) {
                statement.setLong(1, shiftId);
                statement.executeUpdate();
            }
        }
    }

    @Test
    void NFR_04_sourceChangeKeysDistinguishResidentsSharingTheSameSource() throws Exception {
        long id = create(), otherResident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO care_task(resident_id,task_type_id,basis_type,basis_id,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,performer_id,status,data,created_by,created_at) SELECT ?,task_type_id,basis_type,basis_id,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,performer_id,status,data,created_by,created_at FROM care_task WHERE id=?",
                otherResident,
                id);
        context.getBean(com.eldercare.backend.care.api.TaskSourceApi.class).sourceChanged(SourceType.MEAL, sourceId);
        var keys = recorded.stream(com.eldercare.backend.care.api.events.TaskSourceChanged.class)
                .filter(e -> e.sourceId() == sourceId)
                .map(e -> e.eventKey())
                .toList();
        assertThat(keys).hasSize(2).doesNotHaveDuplicates();
    }

    @Test
    void FR_026a_batchCannotBypassTheQualityCheckCommand() throws Exception {
        plan("[" + row(residentId, sourceId, planned).replace("\"MEAL\"", "\"REWORK\"") + "]")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rowErrors[0].codes").value(org.hamcrest.Matchers.hasItem("INVALID_BASIS")));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE shift_id=?", Integer.class, shiftId))
                .isZero();
    }

    @Test
    void FR_017_shiftFloorCannotBeUsedToAuthorizeAForeignResident() throws Exception {
        long other = TestUsers.nextId(), foreignFloor = TestUsers.nextId();
        residents.resident(other, "[Họ tên kiểm thử]", Gender.MALE, ResidentStatus.IN_RESIDENCE);
        contracts.period(
                other, new ResidenceTypePeriod(ResidenceType.LONG_TERM_RESIDENTIAL, time.today(), Optional.empty()));
        beds.allocate(
                other, new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), foreignFloor));
        String r = row(other, sourceId, planned)
                .replace("\"MEAL\"", "\"AD_HOC\"")
                .replace(",\"performerId\":" + caregiver.staffId(), "")
                .replace("\"shared\":false", "\"shared\":true");
        plan("[" + r + "]").andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE shift_id=?", Integer.class, shiftId))
                .isZero();
    }

    private long sourceItem(int revision, LocalDate from, LocalDate to, String state) {
        long p = TestUsers.nextId(), i = TestUsers.nextId();
        var data = new com.eldercare.backend.care.internal.careplan.domain.CarePlanItemData(
                typeId,
                "Mục phiên bản " + revision,
                "ONCE_PER_SHIFT",
                null,
                null,
                null,
                List.of(),
                List.of(),
                15,
                15,
                Role.CAREGIVER,
                com.eldercare.backend.care.api.dto.ResultType.PERFORMED,
                com.eldercare.backend.care.api.dto.Importance.NORMAL,
                false,
                null);
        jdbc.update(
                "INSERT INTO care_plan(id,resident_id,revision,status,effective_from,created_by,created_at) VALUES(?,?,?,'DRAFT',?,?,?)",
                p,
                residentId,
                revision,
                from,
                manager.accountId(),
                LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE));
        jdbc.update(
                "INSERT INTO care_plan_item(id,care_plan_id,kind,task_type_id,frequency_type,data) VALUES(?,?,'TASK',?,'ONCE_PER_SHIFT',?)",
                i,
                p,
                typeId,
                json.writeValueAsString(data));
        jdbc.update("UPDATE care_plan SET status=?,effective_to=? WHERE id=?", state, to, p);
        return i;
    }

    @Test
    void FR_017_nightShiftSourceIncludesBothBusinessDates() throws Exception {
        var today = time.today();
        shifts.shift(new ShiftView(
                shiftId,
                floor,
                today.minusDays(1).atTime(22, 0).atZone(BusinessTime.ZONE).toInstant(),
                today.atTime(6, 0).atZone(BusinessTime.ZONE).toInstant(),
                true));
        long old = sourceItem(1, today.minusDays(1), today.minusDays(1), "EXPIRED");
        long current = sourceItem(2, today, null, "EFFECTIVE");
        mvc.perform(get("/api/v1/shifts/" + shiftId + "/source-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residents[0].planItems[*].id")
                        .value(org.hamcrest.Matchers.hasItems((int) old, (int) current)));
    }
}
