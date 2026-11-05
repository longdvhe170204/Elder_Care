package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.*;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

/** T033: lệnh thật, MySQL và kiểm quyền thật; chỉ nguồn ngoài M04 dùng fixture của chủ SPI. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
@Import({FR_028a_ReasonCatalogHttpIT.Users.class, FakeM15Config.class, FakeM01Config.class, FakeM04Config.class})
abstract class CareResultTestBase extends MySqlIntegrationTestBase {
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
    ApplicationEvents events;

    long resident, floor, shift, type, task, performer;

    /** Trưởng tầng được giao của tầng, cũng là người thực hiện mặc định; test đổi người dùng rồi đặt lại. */
    TestUsers.TestUser baseManager;

    @BeforeEach
    void fixture() {
        resident = TestUsers.nextId();
        floor = TestUsers.nextId();
        shift = TestUsers.nextId();
        type = TestUsers.nextId();
        task = TestUsers.nextId();
        var manager = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
        baseManager = manager;
        performer = manager.staffId();
        user.set(manager.user());
        assignments.assign(
                performer,
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(86400),
                        null));
        residents.resident(resident, "[Họ tên kiểm thử]", Gender.MALE, ResidentStatus.IN_RESIDENCE);
        contracts.period(
                resident,
                new ResidenceTypePeriod(
                        ResidenceType.LONG_TERM_RESIDENTIAL, time.today().minusDays(1), Optional.empty()));
        beds.allocate(
                resident, new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floor));
        shifts.shift(new ShiftView(
                shift, floor, time.now().minusSeconds(3600), time.now().plusSeconds(3600), true));
        shifts.members(shift, List.of(new ShiftMemberView(performer, Role.FLOOR_MANAGER)));
        shifts.onShift(performer, time.now(), true);
        shifts.qualified(performer, Set.of(), time.today(), true);
        jdbc.update(
                "INSERT INTO care_task(id,resident_id,task_type_id,basis_type,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,performer_id,status,data,created_by,created_at) VALUES(?,?,?,'AD_HOC',?,?,?,?,?,'NORMAL','CAREGIVER',?,'DUE',?, ?,?)",
                task,
                resident,
                type,
                shift,
                floor,
                local(time.now().minusSeconds(300)),
                local(time.now().minusSeconds(900)),
                local(time.now().plusSeconds(900)),
                performer,
                "{\"publicTaskTypeId\":" + type
                        + ",\"taskTypeName\":\"Uống nước kiểm thử\",\"content\":\"Kiểm thử\",\"resultType\":\"ML\",\"billable\":false,\"upperLimit\":500,\"certificates\":[]}",
                user.accountId(),
                local(time.now()));
    }

    LocalDateTime local(Instant at) {
        return LocalDateTime.ofInstant(at, BusinessTime.ZONE);
    }

    ResultActions record(String body) throws Exception {
        return mvc.perform(post("/api/v1/care-tasks/" + task + "/record")
                .header("Idempotency-Key", "record-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    ResultActions notPerform(String body) throws Exception {
        return mvc.perform(post("/api/v1/care-tasks/" + task + "/not-perform")
                .header("Idempotency-Key", "not-perform-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    String storedStatus() {
        return jdbc.queryForObject("SELECT status FROM care_task WHERE id=?", String.class, task);
    }

    void data(String resultType, boolean billable) {
        jdbc.update(
                "UPDATE care_task SET data=JSON_SET(data,'$.resultType',?,'$.billable',CAST(? AS JSON),'$.serviceId',?) WHERE id=?",
                resultType,
                Boolean.toString(billable),
                TestUsers.nextId(),
                task);
    }
}
