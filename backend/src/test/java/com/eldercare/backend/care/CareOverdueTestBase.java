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

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.care.internal.task.persistence.CareTaskStore taskStore;

    long residentId, floor, shiftId, typeId, sourceId;
    Instant planned;
    TestUsers.TestUser manager, caregiver;

    @BeforeEach
    void fixture() {
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
        shifts.qualified(caregiver.staffId(), Set.of(), time.today(), true);
        meals.validation(SourceType.MEAL, sourceId, residentId, planned, new SourceValidation(true, true, true, false));
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE',?)",
                typeId,
                "Loại kiểm thử " + typeId,
                "{\"group\":\"OTHER\",\"professional\":false,\"resultType\":\"PERFORMED\",\"defaultRole\":\"CAREGIVER\",\"defaultImportance\":\"NORMAL\",\"billable\":false,\"certificates\":[]}");
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

    /** Công việc thứ hai cùng người, căn cứ khác (tránh trùng DBR-12). */
    long createAnother() throws Exception {
        long other = TestUsers.nextId();
        var at = planned.plusSeconds(600);
        meals.validation(SourceType.MEAL, other, residentId, at, new SourceValidation(true, true, true, false));
        shifts.onShift(caregiver.staffId(), at, true);
        var result = plan("[" + row(residentId, other, at) + "]")
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
