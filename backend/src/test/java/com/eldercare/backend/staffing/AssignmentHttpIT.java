package com.eldercare.backend.staffing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.care.*;
import com.eldercare.backend.support.fake.resident.*;
import com.eldercare.backend.support.fake.roombedallocation.*;
import com.eldercare.backend.support.fake.roombedallocation.FakeLocations;
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
@Import({FakeM01Config.class, FakeLocations.Config.class, FakeM04Config.class})
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
                "INSERT INTO catalog_item(catalog_type,module,name,status,attributes) SELECT 'M09.SHIFT_TEMPLATE','M09',src.name,'ACTIVE',JSON_OBJECT('createdAt',DATE_FORMAT(UTC_TIMESTAMP(6),'%Y-%m-%dT%H:%i:%s.%fZ'),'startTime',TIME_FORMAT(src.start_time,'%H:%i:%s'),'endTime',TIME_FORMAT(src.end_time,'%H:%i:%s'),'scopeType',src.scope_type,'requiresHandover',IF(src.requires_handover,CAST('true' AS JSON),CAST('false' AS JSON)),'originalName',src.name) FROM (SELECT '[Mẫu HTTP phân công] #"
                        + com.eldercare.backend.support.TestUsers.nextId()
                        + "' AS name,'07:00' AS start_time,'15:00' AS end_time,'FLOOR' AS scope_type,FALSE AS requires_handover) src");
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
}
