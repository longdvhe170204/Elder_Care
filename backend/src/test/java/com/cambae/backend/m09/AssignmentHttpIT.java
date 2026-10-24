package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.support.*;
import com.cambae.backend.support.fake.m01.*;
import com.cambae.backend.support.fake.m03.*;
import com.cambae.backend.support.fake.m04.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

/** T022: HTTP thật, xác thực thật, chỉ nguồn chưa có bản cài dùng fake chung của chủ interface. */
@AutoConfigureMockMvc
@Import({FakeM01Config.class, FakeM03Config.class, FakeM04Config.class})
class AssignmentHttpIT extends MySqlIntegrationTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    JsonMapper json;

    @Autowired
    FakeLocations locations;

    @Autowired
    FakeResidents residents;

    private String manager, workerToken;
    private long worker, room, floor, shift, schedule;
    private Instant from, to;

    private long insert(String sql, Object... args) {
        return AssignmentFixtures.insert(jdbc, sql, args);
    }

    private long staff(Role role) {
        var account = new TestUsers(jdbc, time).staff(role);
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?,'[Nhân viên HTTP phân công]','Chức danh',?)",
                account.staffId(),
                account.accountId());
        return account.staffId();
    }

    @BeforeEach
    void setup() {
        var users = new TestUsers(jdbc, time);
        manager = TestUsers.bearer(
                users.session(users.staff(Role.NURSING_HOME_MANAGER).accountId()));
        worker = staff(Role.CAREGIVER);
        workerToken = TestUsers.bearer(users.session(
                jdbc.queryForObject("SELECT account_id FROM staff_profile WHERE id=?", Long.class, worker)));
        floor = TestUsers.nextId();
        room = TestUsers.nextId();
        locations.floor(floor, TestUsers.nextId()).room(room, floor);
        from = time.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusSeconds(3600);
        to = from.plusSeconds(8 * 3600);
        long template = insert(
                "INSERT INTO shift_template(name,start_time,end_time,scope_type,requires_handover) VALUES ('[Mẫu HTTP phân công]','07:00','15:00','FLOOR',FALSE)");
        schedule = insert(
                "INSERT INTO shift_schedule(month,scope_type,scope_id) VALUES (?,'FLOOR',?)",
                YearMonth.from(from.atZone(BusinessTime.ZONE)).toString(),
                floor);
        shift = insert(
                "INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover) VALUES (?,?,'FLOOR',?,?,?,FALSE)",
                schedule,
                template,
                floor,
                ShiftStore.sql(from),
                ShiftStore.sql(to));
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, worker);
    }

    private MockHttpServletRequestBuilder command(MockHttpServletRequestBuilder request, String body) {
        return request.header("Authorization", manager)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private String body() {
        return "{\"staffId\":" + worker + ",\"targetType\":\"ROOM\",\"targetId\":" + room
                + ",\"role\":\"CAREGIVER\",\"assignmentKind\":\"PRIMARY\"}";
    }

    private long create() throws Exception {
        var response = mvc.perform(command(post("/api/v1/shifts/{id}/assignments", shift), body()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        return json.readTree(response.getContentAsString()).get("id").asLong();
    }

    @Test
    void FR_031_previewHasNoIdAndDoesNotCreateRowsOrAudit() throws Exception {
        mvc.perform(post("/api/v1/shifts/{id}/assignments/preview", shift)
                        .header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.warnings").isArray())
                .andExpect(jsonPath("$.id").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_assignment WHERE shift_id=?", Integer.class, shift))
                .isZero();
        mvc.perform(get("/api/v1/shifts/{id}", shift).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedCommands", org.hamcrest.Matchers.hasItem("createAssignment")));
    }

    @Test
    void FR_025_createIsIdempotentAndDraftCanBeListedThenDeleted() throws Exception {
        String key = UUID.randomUUID().toString();
        var request = post("/api/v1/shifts/{id}/assignments", shift)
                .header("Authorization", manager)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body());
        var response =
                mvc.perform(request).andExpect(status().isCreated()).andReturn().getResponse();
        long id = json.readTree(response.getContentAsString()).get("id").asLong();
        mvc.perform(request).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/shifts/{id}/assignments", shift).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM staff_assignment WHERE shift_id=?", Integer.class, shift))
                .isOne();
        mvc.perform(delete("/api/v1/assignments/{id}", id).header("Authorization", manager))
                .andExpect(status().isNoContent());
    }

    @Test
    void FR_027_cancelNeedsReasonAndEmployeeCannotCreate() throws Exception {
        jdbc.update("UPDATE shift_schedule SET status='PUBLISHED' WHERE id=?", schedule);
        jdbc.update("UPDATE work_shift SET status='PUBLISHED' WHERE id=?", shift);
        long id = create();
        mvc.perform(command(post("/api/v1/assignments/{id}/cancel", id), "{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        mvc.perform(command(post("/api/v1/assignments/{id}/cancel", id), "{\"reason\":\"Đổi kế hoạch phân công\"}"))
                .andExpect(status().isOk());
        mvc.perform(command(post("/api/v1/assignments/{id}/cancel", id), "{\"reason\":\"Gửi lại lệnh\"}"))
                .andExpect(status().isConflict());
        // 002 trả 404 khi ca không thuộc phạm vi dữ liệu của khóa quyền này; không tiết lộ đối tượng khi từ chối ghi.
        mvc.perform(post("/api/v1/shifts/{id}/assignments", shift)
                        .header("Authorization", workerToken)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isNotFound());
    }

    @Test
    void FR_027_activeAssignmentCanBeEndedByManager() throws Exception {
        jdbc.update("UPDATE shift_schedule SET status='PUBLISHED' WHERE id=?", schedule);
        jdbc.update(
                "UPDATE work_shift SET status='IN_PROGRESS',starts_at=?,ends_at=? WHERE id=?",
                ShiftStore.sql(time.now().minusSeconds(60)),
                ShiftStore.sql(time.now().plusSeconds(3600)),
                shift);
        long id = create();
        mvc.perform(command(post("/api/v1/assignments/{id}/end", id), "{\"reason\":\"Đổi người phụ trách\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM staff_assignment WHERE id=?", String.class, id))
                .isEqualTo("ENDED");
    }

    @Test
    void FR_024_floorManagerCommandsUseDatesAndRequireReasonToShorten() throws Exception {
        long person = staff(Role.FLOOR_MANAGER);
        String body = "{\"staffId\":" + person + ",\"floorAreaId\":" + floor + ",\"from\":\"" + time.today() + "\"}";
        var response = mvc.perform(command(post("/api/v1/floor-manager-assignments"), body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        String location = response.getHeader("Location");
        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
        mvc.perform(get("/api/v1/floor-manager-assignments")
                        .param("floorAreaId", Long.toString(floor))
                        .header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].from").value(time.today().toString()));
        mvc.perform(command(
                        post("/api/v1/floor-manager-assignments/{id}/end", id),
                        "{\"to\":\"" + time.today() + "\",\"reason\":\"Nghỉ dài ngày\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void FR_033_employeeCannotReadUnpublishedDraftAssignments() throws Exception {
        create();
        mvc.perform(get("/api/v1/shifts/{id}/assignments", shift).header("Authorization", workerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void FR_032_copyEndpointReturnsPerRowResults() throws Exception {
        create();
        long source = shift;
        long template = jdbc.queryForObject("SELECT template_id FROM work_shift WHERE id=?", Long.class, shift);
        shift = insert(
                "INSERT INTO work_shift(schedule_id,template_id,scope_type,scope_id,starts_at,ends_at,requires_handover) VALUES (?,?,'FLOOR',?,?,?,FALSE)",
                schedule,
                template,
                floor,
                ShiftStore.sql(to),
                ShiftStore.sql(to.plusSeconds(3600)));
        jdbc.update("INSERT INTO shift_staff(shift_id,staff_id) VALUES (?,?)", shift, worker);
        mvc.perform(command(post("/api/v1/shifts/{id}/copy-assignments", shift), "{\"sourceShiftId\":" + source + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.copiedIds.length()").value(1))
                .andExpect(jsonPath("$.notCopied").isEmpty());
    }
}
