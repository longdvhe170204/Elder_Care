package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.*;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.staffing.api.events.*;
import com.eldercare.backend.staffing.internal.ratio.service.StaffingRatioService;
import com.eldercare.backend.staffing.internal.shift.domain.*;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.staffing.internal.shift.service.ShiftScheduleService;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.fake.administration.*;
import com.eldercare.backend.support.fake.admission.*;
import com.eldercare.backend.support.fake.roombedallocation.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
@Import({
    FakeM02Config.class,
    FakeLocations.Config.class,
    FakeM15Config.class,
    com.eldercare.backend.support.fake.care.FakeM04Config.class,
    com.eldercare.backend.support.fake.resident.FakeCareLevelChangeRequests.class,
    SwitchableUserConfig.class
})
class StaffingRatioIT extends MySqlIntegrationTestBase {
    private static final java.util.concurrent.atomic.AtomicInteger CASES =
            new java.util.concurrent.atomic.AtomicInteger();

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
    SwitchableUser current;

    @Autowired
    FakeLocations locations;

    @Autowired
    FakeContractSchedules attendance;

    @Autowired
    FakeAssignments duties;

    @Autowired
    ShiftScheduleService schedules;

    @Autowired
    StaffingRatioService ratios;

    @Autowired
    com.eldercare.backend.support.fake.resident.FakeCareLevelChangeRequests levelRequests;

    @Autowired
    com.eldercare.backend.staffing.internal.staff.service.StaffEmploymentEffects employment;

    @Autowired
    ApplicationEventPublisher events;

    @Autowired
    TransactionTemplate tx;

    TestUsers users;
    TestUsers.TestUser manager;
    String bearer;
    long floor, room, template, schedule;
    Instant from, to;
    Instant clockBase;
    List<Long> members;

    @BeforeEach
    void setup() throws Exception {
        clockBase = Instant.parse("2026-10-24T00:00:00Z").plusSeconds(60L * CASES.incrementAndGet());
        clock.setTo(clockBase);
        users = new TestUsers(jdbc, time);
        manager = user(Role.NURSING_HOME_MANAGER);
        current.set(manager.user());
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Quản lý kiểm thử tỷ lệ]','Quản lý viện',?)",
                manager.staffId(),
                manager.accountId());
        bearer = TestUsers.bearer(users.session(manager.accountId()));
        floor = TestUsers.nextId();
        room = TestUsers.nextId();
        locations.floor(floor, null).room(room, floor).daycareRestLocation(null);
        attendance.clear();
        from = Instant.parse("2027-11-01T00:00:00Z");
        to = from.plus(Duration.ofHours(8));
        members = List.of(
                staff(Role.NURSING_STAFF), staff(Role.CAREGIVER), staff(Role.CAREGIVER), staff(Role.NURSING_STAFF));
        template = schedules
                .createTemplate(new TemplateInput(
                        "[Mẫu tỷ lệ tích hợp] #" + TestUsers.nextId(),
                        LocalTime.of(7, 0),
                        LocalTime.of(15, 0),
                        "FLOOR",
                        false,
                        true))
                .id();
        schedule = schedules
                .createSchedule(YearMonth.of(2027, 11), "FLOOR", floor, new Target("M09.SCHEDULE", null, null, floor))
                .id();
        duties.assign(
                manager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.startOfDay(LocalDate.of(2027, 11, 1)),
                        time.startOfDay(LocalDate.of(2027, 12, 1))));
        parameter("CFG-M09-01", Map.of(Long.toString(template), 8));
        parameter("CFG-M09-12", Map.of("BASIC", 1, "REGULAR", 1.5, "SPECIAL", 2.5));
    }

    @AfterEach
    void reset() throws Exception {
        clock.setTo(clockBase.plusSeconds(30));
        bearer = TestUsers.bearer(users.session(manager.accountId()));
        current.set(manager.user());
        attendance.available(true);
        restore("CFG-M09-01");
        restore("CFG-M09-12");
        clock.reset();
    }

    private void parameter(String code, Object value) throws Exception {
        changeParameter(code, Map.of("value", value, "reason", "Cấu hình tỷ lệ cho kiểm thử"));
    }

    private void restore(String code) throws Exception {
        changeParameter(code, Map.of("restoreDefault", true, "reason", "Khôi phục tham số sau kiểm thử"));
    }

    private void changeParameter(String code, Object body) throws Exception {
        clock.advanceBy(Duration.ofMillis(1));
        long version = jdbc.queryForObject("SELECT version FROM parameter WHERE code=?", Long.class, code);
        mvc.perform(post("/api/v1/parameters/{code}/change-value", code)
                        .header("Authorization", bearer)
                        .header("If-Match", "\"" + version + "\"")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json")
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private long staff(Role... roles) {
        var u = user(roles);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên tỷ lệ]','Chăm sóc',?)",
                u.staffId(),
                u.accountId());
        return u.staffId();
    }

    private TestUsers.TestUser user(Role... roles) {
        // Các test import/tạo hồ sơ trước đó có thể đã dùng AUTO_INCREMENT vượt bộ đếm TestUsers.
        long used = jdbc.queryForObject("SELECT COALESCE(MAX(id),0) FROM staff_profile", Long.class);
        while (TestUsers.nextId() <= used) {
            /* Dành id chưa có hồ sơ, giữ đúng liên kết tài khoản. */
        }
        return users.staff(roles);
    }

    private long resident(CareLevel level) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident(id,profile_code,full_name,date_of_birth,gender,status,status_since,current_care_level,created_at,created_by) VALUES (?,?,'[Họ tên NCT tỷ lệ]','1940-01-01','FEMALE','IN_RESIDENCE',?,?,?,?)",
                id,
                "RATIO-" + id,
                ShiftStore.sql(time.now()),
                level.name(),
                ShiftStore.sql(time.now()),
                manager.accountId());
        return id;
    }

    private long inpatient(CareLevel level) {
        long id = resident(level);
        locations.stay(id, room, from.minusSeconds(86400), null);
        return id;
    }

    private void history(long resident, String before, String next, String command, Instant at) {
        // Lịch sử trạng thái của 001 ở status_history từ 001 T084 (data-model 001 mục 0.1)
        com.eldercare.backend.support.ResidentHistoryRows.insertStatus(
                jdbc,
                null,
                resident,
                before,
                next,
                command,
                ShiftStore.sql(at),
                manager.accountId(),
                true,
                "Mốc trạng thái kiểm thử");
    }

    private long save() {
        return schedules
                .addShift(
                        schedule,
                        "\"0\"",
                        new ShiftInput(
                                template,
                                LocalDate.of(2027, 11, 1),
                                from,
                                to,
                                members.stream()
                                        .map(id -> new ShiftInput.Member(id, false))
                                        .toList(),
                                null,
                                false,
                                null),
                        Target.UNSPECIFIED)
                .shiftId();
    }

    /** 015 FR-021 (f), FR-027 (015 T002): xem trước tỷ lệ khi một nhân viên rời hoặc vào ca; chỉ đọc, không ghi lần tính. */
    @Test
    void FR_034_beforeAfterPreview_leavingAndJoiningStaff_doesNotRecordCalculation() {
        inpatient(CareLevel.SPECIAL);
        inpatient(CareLevel.BASIC);
        long id = save();
        int calculations = ratios.history(id).size();
        long joining = staff(Role.CAREGIVER);

        var leaving = ratios.beforeAfter(id, members.get(0), null);
        var swapped = ratios.beforeAfter(id, members.get(0), joining);
        var added = ratios.beforeAfter(id, null, joining);

        assertThat(leaving).containsEntry("shiftId", id);
        assertThat(side(leaving, "before")).containsEntry("staffCount", 4);
        assertThat(side(leaving, "after")).containsEntry("staffCount", 3);
        assertThat(((BigDecimal) (side(leaving, "after")).get("totalWeight"))).isEqualByComparingTo("3.5");
        assertThat(side(swapped, "after")).containsEntry("staffCount", 4);
        assertThat(side(added, "after")).containsEntry("staffCount", 5);
        assertThat(ratios.history(id)).hasSize(calculations);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> side(Map<String, Object> preview, String key) {
        return (Map<String, Object>) preview.get(key);
    }
}
