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
class BR_18_ShiftTaskPlanningIT extends MySqlIntegrationTestBase {
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
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE',?)",
                typeId,
                "Loại kiểm thử " + typeId,
                "{\"group\":\"OTHER\",\"professional\":false,\"resultType\":\"PERFORMED\",\"defaultRole\":\"CAREGIVER\",\"defaultImportance\":\"NORMAL\",\"billable\":false,\"certificates\":[]}");
        // 011 T042: căn cứ là mục kế hoạch chăm sóc Hiệu lực (nguồn thật của 005), không còn bản giả MEAL.
        sourceId =
                effectiveItem(1, time.today(), planned.atZone(BusinessTime.ZONE).toLocalTime());
    }

    @AfterEach
    void restoreClock() {
        clock.reset();
    }

    String row(long resident, long source, Instant at) {
        return "{\"residentId\":" + resident + ",\"taskTypeId\":" + typeId
                + ",\"basis\":{\"type\":\"CARE_PLAN_ITEM\",\"id\":"
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
    void BR_18_validBatchPersistsAndCanBeRead() throws Exception {
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
    void Q_444_shiftLeadNurseCannotPlanShiftTasks() throws Exception {
        var nurse = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF);
        shifts.shiftLead(shiftId, nurse.staffId());
        assignments.assign(
                nurse.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_LEAD,
                        floor,
                        null,
                        null,
                        shiftId,
                        time.now().minusSeconds(3600),
                        time.now().plusSeconds(14400)));
        user.set(nurse.user());
        plan("[" + row(residentId, sourceId, planned) + "]").andExpect(status().isForbidden());
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
    void Q452_sourceDataOffersOnlyOccurrencesOfThePlanEffectiveOnEachDate() throws Exception {
        var today = time.today();
        var tomorrow = today.plusDays(1);
        var starts = today.atTime(20, 0).atZone(BusinessTime.ZONE).toInstant();
        var ends = tomorrow.atTime(12, 0).atZone(BusinessTime.ZONE).toInstant();
        shifts.shift(new ShiftView(shiftId, floor, starts, ends, true));
        jdbc.update(
                "UPDATE care_plan SET status='EXPIRED',effective_to=? WHERE resident_id=? AND revision=1",
                today,
                residentId);
        var hour = LocalTime.of(6, 30);
        long tomorrowItem = effectiveItem(2, tomorrow, hour);
        var tomorrowAt = tomorrow.atTime(hour).atZone(BusinessTime.ZONE).toInstant();

        mvc.perform(get("/api/v1/shifts/" + shiftId + "/source-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residents[0].planItems.length()").value(2))
                .andExpect(jsonPath("$.residents[0].planOccurrences.length()").value(1))
                .andExpect(jsonPath("$.residents[0].planOccurrences[0].itemId").value(tomorrowItem))
                .andExpect(
                        jsonPath("$.residents[0].planOccurrences[0].plannedAt").value(tomorrowAt.toString()));
    }

    ResultActions monthCommand(String path, String body) throws Exception {
        return mvc.perform(post("/api/v1/care-tasks/month-drafts" + path)
                .header("Idempotency-Key", "month-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    String month() {
        return "{\"floorAreaId\":" + floor + ",\"month\":\""
                + time.today().toString().substring(0, 7) + "\"}";
    }

    int countStatus(String status) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM care_task WHERE resident_id=? AND status=?", Integer.class, residentId, status);
    }

    @Test
    void Q453_monthCopyCreatesDraftsOnceHiddenFromChecklistThenPublished() throws Exception {
        monthCommand("", month())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1));
        assertThat(countStatus("DRAFT")).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT performer_id FROM care_task WHERE resident_id=? AND status='DRAFT'",
                        Long.class,
                        residentId))
                .isEqualTo(caregiver.staffId());
        // Chép lại không tạo trùng
        monthCommand("", month())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0));
        assertThat(countStatus("DRAFT")).isEqualTo(1);
        mvc.perform(get("/api/v1/care-tasks/month-drafts")
                        .param("floorAreaId", Long.toString(floor))
                        .param("month", time.today().toString().substring(0, 7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.residentId==" + residentId + ")]").isNotEmpty());
        // Nhân viên không thấy việc Nháp trên checklist
        user.set(caregiver.user());
        mvc.perform(get("/api/v1/checklist").param("shiftId", Long.toString(shiftId)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.tasks[?(@.residentId==" + residentId + ")]").isEmpty());
        user.set(manager.user());

        monthCommand("/publish", month())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(1));
        assertThat(countStatus("DRAFT")).isZero();
        assertThat(countStatus("NOT_YET_DUE")).isEqualTo(1);
    }

    @Test
    void Q453_draftCanBeRemovedBeforePublish() throws Exception {
        monthCommand("", month()).andExpect(status().isOk());
        long id = jdbc.queryForObject(
                "SELECT id FROM care_task WHERE resident_id=? AND status='DRAFT'", Long.class, residentId);
        mvc.perform(post("/api/v1/care-tasks/month-drafts/" + id + "/discard").header("Idempotency-Key", "del-" + id))
                .andExpect(status().isOk());
        assertThat(countStatus("DRAFT")).isZero();
        monthCommand("/publish", month()).andExpect(jsonPath("$.published").value(0));
    }

    @Test
    void Q453_onlyFloorManagerCopiesMonth() throws Exception {
        user.set(caregiver.user());
        monthCommand("", month()).andExpect(status().isForbidden());
        assertThat(countStatus("DRAFT")).isZero();
    }

    private long effectiveItem(int revision, LocalDate effectiveFrom, LocalTime due) {
        long planId = TestUsers.nextId(), itemId = TestUsers.nextId();
        var data = new com.eldercare.backend.care.internal.careplan.domain.CarePlanItemData(
                typeId,
                "Mục kiểm thử",
                "TIMES_PER_DAY",
                null,
                1,
                null,
                List.of(due),
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
                planId,
                residentId,
                revision,
                effectiveFrom,
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

    @Autowired
    FakeM04Switches fakeSwitches;
}
