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
import com.eldercare.backend.support.SwitchableUserConfig;
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
@Import({SwitchableUserConfig.class, FakeM15Config.class, FakeM01Config.class, FakeM04Config.class})
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
}
