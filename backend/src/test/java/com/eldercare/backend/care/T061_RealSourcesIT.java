package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.care.api.spi.ShiftStaffPort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.RealCareFixture;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.time.Instant;
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
     * US5, US9: công việc quá khung chuyển Quá hạn; thời khóa biểu của người cao tuổi gộp công việc với buổi hoạt động đã
     * đăng ký đọc qua nguồn thật của 014 ({@code ActivityTimetableSource}) và nguồn thuốc thật của 006 (không có liều).
     */
    @Test
    void US5_US9_overdueAndTimetableWithRealActivitySource() throws Exception {
        long task = json.readTree(plan(caregiver.staffId())
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .get(0)
                .get("id")
                .asLong();
        long activity = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO simple_record(id,kind,module,status,content,created_at,created_by) VALUES(?,'M04.ACTIVITY','M04','ACTIVE',?,NOW(6),1)",
                activity,
                "{\"name\":\"Hát cùng nhau\",\"scope\":\"FACILITY\",\"maxParticipants\":20,\"charged\":false,\"groupActivity\":true}");
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

        user.set(manager.user());
        mvc.perform(get("/api/v1/residents/" + resident + "/timetable")
                        .param("date", time.today().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("ACTIVITY_SESSION"))
                .andExpect(jsonPath("$[0].description").value("Hát cùng nhau"))
                .andExpect(jsonPath("$[1].source").value("CARE_TASK"));

        Instant late = planned.plus(Duration.ofMinutes(20));
        clock.setTo(late);
        clockProcessor.advance(task, late);
        clockProcessor.advance(task, late);
        mvc.perform(get("/api/v1/care-tasks/" + task))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OVERDUE"));
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
                "{\"name\":\"Dưỡng sinh\",\"scope\":\"FACILITY\",\"maxParticipants\":20,\"charged\":false,\"groupActivity\":true}");
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

    @Test
    void US2_US4_realShiftAssignmentBedAndContract_planThenRecord() throws Exception {
        assertThat(shiftStaff.members(shift)).extracting(m -> m.staffId()).containsExactly(caregiver.staffId());
        assertThat(shiftStaff.assignments(resident, shift))
                .extracting(a -> a.staffId())
                .containsExactly(caregiver.staffId());

        plan(outsider.staffId()).andExpect(status().is4xxClientError());

        String body = plan(caregiver.staffId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].status").value("NOT_YET_DUE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long task = json.readTree(body).get(0).get("id").asLong();

        clock.setTo(planned);
        // Bộ lập lịch chuyển Chưa đến hạn → Đến hạn theo giờ (CareMinuteJob); gọi đúng công việc này.
        clockProcessor.advance(task, planned);
        user.set(caregiver.user());
        mvc.perform(post("/api/v1/care-tasks/" + task + "/record")
                        .header("Idempotency-Key", "record-" + TestUsers.nextId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/care-tasks/" + task))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }
}
