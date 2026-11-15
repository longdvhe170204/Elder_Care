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
abstract class CareOverdueTestBase extends MySqlIntegrationTestBase {
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

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.care.internal.task.persistence.CareTaskStore taskStore;

    /**
     * Căn cứ là mục kế hoạch chăm sóc Hiệu lực (nguồn thật của 005; 011 T042 bỏ bản giả {@code MEAL}: căn cứ không phải
     * điều các IT quá hạn, bàn giao kiểm). {@code sourceId} có mốc {@code planned} và {@code planned + 10 phút};
     * {@code sameTimeSourceId} là mục khác cùng mốc {@code planned}.
     */
    long residentId, floor, shiftId, typeId, sourceId, sameTimeSourceId;

    Instant planned;
    TestUsers.TestUser manager, caregiver;

    @BeforeEach
    void fixture() {
        clock.setTo(time.today().atTime(8, 0).atZone(BusinessTime.ZONE).toInstant());
        org.mockito.Mockito.doAnswer(
                        invocation -> ((java.util.List<com.eldercare.backend.care.internal.task.domain.CareTask>)
                                        invocation.callRealMethod())
                                .stream().filter(t -> t.floorAreaId() == floor).toList())
                .when(taskStore)
                .waiting(org.mockito.ArgumentMatchers.any());
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
        long planId = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO care_plan(id,resident_id,revision,status,effective_from,created_by,created_at) VALUES(?,?,1,'DRAFT',?,?,?)",
                planId,
                residentId,
                time.today(),
                manager.accountId(),
                LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE));
        sourceId = planItem(planId, planned, planned.plusSeconds(600));
        sameTimeSourceId = planItem(planId, planned);
        jdbc.update("UPDATE care_plan SET status='EFFECTIVE' WHERE id=?", planId);
    }

    private long planItem(long planId, Instant... times) {
        long itemId = TestUsers.nextId();
        var data = new com.eldercare.backend.care.internal.careplan.domain.CarePlanItemData(
                typeId,
                "Nội dung kiểm thử",
                "TIMES_PER_DAY",
                null,
                times.length,
                null,
                Arrays.stream(times)
                        .map(t -> t.atZone(BusinessTime.ZONE).toLocalTime())
                        .toList(),
                List.of(),
                15,
                15,
                Role.CAREGIVER,
                com.eldercare.backend.care.api.dto.ResultType.PERFORMED,
                com.eldercare.backend.care.api.dto.Importance.NORMAL,
                false,
                null);
        jdbc.update(
                "INSERT INTO care_plan_item(id,care_plan_id,kind,task_type_id,frequency_type,data) VALUES(?,?,'TASK',?,'TIMES_PER_DAY',?)",
                itemId,
                planId,
                typeId,
                json.writeValueAsString(data));
        return itemId;
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

    /** Công việc thứ hai cùng người, cùng mục kế hoạch ở mốc khác (không trùng DBR-12). */
    long createAnother() throws Exception {
        var at = planned.plusSeconds(600);
        shifts.onShift(caregiver.staffId(), at, true);
        shifts.qualified(
                caregiver.staffId(), Set.of(), at.atZone(BusinessTime.ZONE).toLocalDate(), true);
        var result = plan("[" + row(residentId, sourceId, at) + "]")
                .andExpect(status().isCreated())
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString())
                .get(0)
                .get("id")
                .asLong();
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
}
