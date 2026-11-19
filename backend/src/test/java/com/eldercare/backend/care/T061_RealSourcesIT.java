package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.care.api.ShiftTaskApi;
import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.health.api.dto.MeasurementFrequency;
import com.eldercare.backend.health.internal.schedule.domain.MeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.NewMeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.ScheduleFrequency;
import com.eldercare.backend.health.internal.schedule.persistence.MeasurementScheduleStore;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.RealCareFixture;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * 005 T061, 008 T043b (chạy lại với bean thật thay T002, 2026-11-11): không có {@code FakeM04Config}, {@code FakeM01Config};
 * ca, thành viên, phân công (008 {@code CareStaffLookup}), giường và phân bổ (003), hợp đồng (004), hồ sơ (001) đều thật.
 * US2: Trưởng tầng lập công việc của ca cho người có mặt trong ca, người không thuộc ca bị chặn; US4: người được giao ghi
 * kết quả, công việc Hoàn thành.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(SwitchableUserConfig.class)
class T061_RealSourcesIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    SwitchableUser user;

    @Autowired
    JsonMapper json;

    @Autowired
    ShiftStaffPort shiftStaff;

    @Autowired
    com.eldercare.backend.care.internal.task.service.CareTaskClockProcessor clockProcessor;

    @Autowired
    ShiftTaskApi shiftTasks;

    @Autowired
    MeasurementScheduleStore schedules;

    @Autowired
    TransactionTemplate tx;

    RealCareFixture fixture;
    RealCareFixture.Place place;
    TestUser manager, caregiver, outsider;
    long resident, shift, typeId;
    Instant planned;

    @BeforeEach
    void realData() {
        clock.setTo(time.today().atTime(8, 0).atZone(BusinessTime.ZONE).toInstant());
        Instant now = time.now();
        fixture = new RealCareFixture(jdbc, time);
        place = fixture.place();
        resident = fixture.residentInBed(place, now.minus(Duration.ofDays(10)));
        manager = fixture.staff(Role.FLOOR_MANAGER);
        caregiver = fixture.staff(Role.CAREGIVER);
        outsider = fixture.staff(Role.CAREGIVER);
        fixture.floorManager(manager.staffId(), place.floor(), now.minus(Duration.ofDays(30)));
        shift = fixture.shift(place.floor(), now.minus(Duration.ofHours(1)), 8);
        fixture.member(shift, caregiver.staffId());
        fixture.lead(shift, manager.staffId(), now.minus(Duration.ofHours(1)));
        fixture.assignResident(
                shift,
                caregiver.staffId(),
                Role.CAREGIVER,
                resident,
                now.minus(Duration.ofHours(1)),
                now.plus(Duration.ofHours(7)));
        planned = now.plus(Duration.ofMinutes(30));
        typeId = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE',?)",
                typeId,
                "Loại thật " + typeId,
                "{\"group\":\"OTHER\",\"professional\":false,\"resultType\":\"PERFORMED\",\"defaultRole\":\"CAREGIVER\",\"defaultImportance\":\"NORMAL\",\"billable\":false,\"certificates\":[]}");
    }

    @AfterEach
    void restore() {
        clock.reset();
        user.reset();
    }

    private String row(long performer) {
        return "[{\"residentId\":" + resident + ",\"taskTypeId\":" + typeId
                + ",\"basis\":{\"type\":\"AD_HOC\"},\"content\":\"Việc phát sinh thật\",\"plannedAt\":\"" + planned
                + "\",\"windowBeforeMinutes\":15,\"windowAfterMinutes\":15,\"importance\":\"NORMAL\",\"performerId\":"
                + performer + ",\"shared\":false}]";
    }

    private ResultActions plan(long performer) throws Exception {
        user.set(manager.user());
        return mvc.perform(post("/api/v1/shifts/" + shift + "/care-tasks/plan")
                .header("Idempotency-Key", "real-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(row(performer)));
    }

    /**
     * 014 T028: công việc căn cứ buổi hoạt động kiểm qua nguồn thật của 014 ({@code ActivityTaskSource}, không có
     * {@code FakeTaskSource} loại {@code ACTIVITY_SESSION}): đăng ký của đúng người, giờ trong buổi thì lập được; đăng ký
     * của người khác bị chặn.
     */
    @Test
    void T028_activitySessionBasis_validatedByRealActivityTaskSource() throws Exception {
        long activity = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO simple_record(id,kind,module,status,content,created_at,created_by) VALUES(?,'M04.ACTIVITY','M04','ACTIVE',?,NOW(6),1)",
                activity,
                "{\"name\":\"Dưỡng sinh\",\"typeId\":1,\"createdByStaffId\":1,\"scope\":\"FACILITY\",\"maxParticipants\":20,\"charged\":false,\"groupActivity\":true}");
        long session = TestUsers.nextId();
        var start = java.time.LocalDateTime.ofInstant(planned.minus(Duration.ofMinutes(5)), BusinessTime.ZONE);
        jdbc.update(
                "INSERT INTO activity_session(id,activity_id,starts_at,ends_at,source,status,created_at,created_by)"
                        + " VALUES(?,?,?,?,'SINGLE','SCHEDULED',NOW(6),1)",
                session,
                activity,
                start,
                start.plusHours(1));
        jdbc.update(
                "INSERT INTO activity_registration(session_id,resident_id,source,registered_at,status) VALUES(?,?,'MANUAL',NOW(6),'REGISTERED')",
                session,
                resident);
        long registration = jdbc.queryForObject(
                "SELECT id FROM activity_registration WHERE session_id=? AND resident_id=?",
                Long.class,
                session,
                resident);
        String body = row(caregiver.staffId())
                .replace("{\"type\":\"AD_HOC\"}", "{\"type\":\"ACTIVITY_SESSION\",\"id\":" + registration + "}");
        user.set(manager.user());
        mvc.perform(post("/api/v1/shifts/" + shift + "/care-tasks/plan")
                        .header("Idempotency-Key", "real-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        long other = fixture.residentInBed(fixture.secondBed(place), time.now().minus(Duration.ofDays(1)));
        mvc.perform(post("/api/v1/shifts/" + shift + "/care-tasks/plan")
                        .header("Idempotency-Key", "real-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"residentId\":" + resident, "\"residentId\":" + other)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rowErrors[0].codes").value(org.hamcrest.Matchers.hasItem("INVALID_BASIS")));
    }

    /**
     * 007 T023 (FR-004, research S-05): căn cứ lịch đo kiểm qua nguồn thật của 007 ({@code MeasurementTaskSource}): mốc
     * thuộc chuỗi mốc đo của lịch, đúng người thì lập được; lịch mức Bắt buộc mà lập công việc mức Thường bị chặn (005
     * FR-023); lịch của người khác bị chặn.
     */
    @Test
    void T023_measurementBasis_validatedByRealMeasurementTaskSource() throws Exception {
        var local = planned.atZone(BusinessTime.ZONE).toLocalTime();
        var frequency = new ScheduleFrequency(MeasurementFrequency.CLOCK_TIMES, List.of(local), null, null, Set.of());
        long schedule = schedules.insert(
                new MeasurementSchedule(
                        0,
                        resident,
                        new NewMeasurementSchedule(
                                Set.of(TestUsers.nextId()),
                                frequency,
                                time.now().minus(Duration.ofDays(1)),
                                null,
                                "MANDATORY",
                                null),
                        "EFFECTIVE",
                        null,
                        null,
                        null,
                        0),
                manager.staffId(),
                time.now());
        String body = row(caregiver.staffId())
                .replace("{\"type\":\"AD_HOC\"}", "{\"type\":\"MEASUREMENT\",\"id\":" + schedule + "}");
        user.set(manager.user());

        mvc.perform(post("/api/v1/shifts/" + shift + "/care-tasks/plan")
                        .header("Idempotency-Key", "real-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rowErrors[0].codes").value(org.hamcrest.Matchers.hasItem("INVALID_BASIS")));

        mvc.perform(post("/api/v1/shifts/" + shift + "/care-tasks/plan")
                        .header("Idempotency-Key", "real-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"importance\":\"NORMAL\"", "\"importance\":\"MANDATORY\"")))
                .andExpect(status().isCreated());

        long other = fixture.residentInBed(fixture.secondBed(place), time.now().minus(Duration.ofDays(1)));
        mvc.perform(post("/api/v1/shifts/" + shift + "/care-tasks/plan")
                        .header("Idempotency-Key", "real-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"importance\":\"NORMAL\"", "\"importance\":\"MANDATORY\"")
                                .replace("\"residentId\":" + resident, "\"residentId\":" + other)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.rowErrors[0].codes").value(org.hamcrest.Matchers.hasItem("INVALID_BASIS")));
    }

    /**
     * 005 T056 (contracts 1.4 {@code khiVangCa}, {@code khiNghiViec}; BR-M04-21) với {@code ShiftStaffPort} thật của 008
     * ({@code CareStaffLookup}): vắng ca làm công việc chưa đóng của người đó trong ca thành việc chung; nghỉ việc làm công
     * việc chưa đóng ở ca còn chưa kết thúc thành việc chung. Lệnh của 008 gọi trong transaction của mình (MANDATORY).
     */
    @Test
    void T056_absenceAndResignation_shareOpenTasks_withRealShiftStaffPort() throws Exception {
        plan(caregiver.staffId()).andExpect(status().isCreated());
        long first = lastTaskOf(caregiver.staffId());

        tx.executeWithoutResult(s -> shiftTasks.onAbsence(caregiver.staffId(), shift));

        assertThat(performerOf(first)).isNull();

        plan(caregiver.staffId()).andExpect(status().isCreated());
        long second = lastTaskOf(caregiver.staffId());

        tx.executeWithoutResult(s -> shiftTasks.onResignation(caregiver.staffId(), time.now()));

        assertThat(performerOf(second)).isNull();
    }

    private long lastTaskOf(long performer) {
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM care_task WHERE shift_id=? AND performer_id=?", Long.class, shift, performer);
    }

    private Long performerOf(long task) {
        return jdbc.queryForObject("SELECT performer_id FROM care_task WHERE id=?", Long.class, task);
    }
}
