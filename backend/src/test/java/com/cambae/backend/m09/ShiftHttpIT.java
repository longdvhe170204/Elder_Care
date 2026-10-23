package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

/** T016: luồng HTTP và quyền thật, nguồn nhiệm vụ tương lai dùng bản giả do 002 khai báo. */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class ShiftHttpIT extends MySqlIntegrationTestBase {
    private static final AtomicInteger MONTHS = new AtomicInteger();

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    JsonMapper json;

    private String manager;
    private String month;
    private long worker;
    private String nurse;

    @BeforeEach
    void setup() {
        var users = new TestUsers(jdbc, time);
        manager = TestUsers.bearer(
                users.session(users.staff(Role.NURSING_HOME_MANAGER).accountId()));
        var staff = users.staff(Role.NURSING_STAFF);
        worker = staff.staffId();
        nurse = TestUsers.bearer(users.session(staff.accountId()));
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,account_id) VALUES (?, '[Nhân viên HTTP ca]', 'NURSING_STAFF', ?)",
                worker,
                staff.accountId());
        month = java.time.YearMonth.of(2038, 1)
                .plusMonths(MONTHS.incrementAndGet())
                .toString();
    }

    private MockHttpServletRequestBuilder command(MockHttpServletRequestBuilder request, String body) {
        return request.header("Authorization", manager)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private long created(MockHttpServletRequestBuilder request) throws Exception {
        String location = mvc.perform(request)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private long template() throws Exception {
        return created(
                command(
                        post("/api/v1/shift-templates"),
                        "{\"name\":\"[Mẫu HTTP]\",\"startTime\":\"07:00\",\"endTime\":\"15:00\",\"scopeType\":\"FACILITY\",\"handoverRequired\":false}"));
    }

    @Test
    void FR_011_templateReturnsVersionAndRequiresManager() throws Exception {
        long id = template();
        mvc.perform(get("/api/v1/shift-templates").header("Authorization", manager))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT version FROM shift_template WHERE id=?", Long.class, id))
                .isZero();
        mvc.perform(
                        post("/api/v1/shift-templates")
                                .header("Authorization", nurse)
                                .header("Idempotency-Key", UUID.randomUUID().toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"X\",\"startTime\":\"07:00\",\"endTime\":\"15:00\",\"scopeType\":\"FACILITY\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_012_scheduleAndShiftSupportVersionedEditsAndOwnShiftView() throws Exception {
        long template = template();
        long schedule = created(
                command(post("/api/v1/shift-schedules"), "{\"month\":\"" + month + "\",\"scopeType\":\"FACILITY\"}"));
        mvc.perform(get("/api/v1/shift-schedules").param("month", month).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value(0));
        String body = "{\"shiftTemplateId\":" + template + ",\"date\":\"" + month + "-01\",\"staff\":[{\"staffId\":"
                + worker + "}]}";
        var response = mvc.perform(command(
                        post("/api/v1/shift-schedules/{id}/shifts", schedule).header("If-Match", "\"0\""), body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse();
        long id = json.readTree(response.getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/v1/shifts/{id}", id).header("Authorization", manager))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.allowedCommands").isArray());
        mvc.perform(command(put("/api/v1/shifts/{id}", id).header("If-Match", "\"9\""), body))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.currentValue").exists());
        mvc.perform(command(post("/api/v1/shift-schedules/{id}/publish", schedule), "{\"reason\":\"Đã kiểm tra\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/me/shifts")
                        .param("from", month + "-01")
                        .param("to", month + "-02")
                        .header("Authorization", nurse))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/v1/shift-schedules").param("month", month).header("Authorization", nurse))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/shift-schedules/{id}/publication-preview", schedule)
                        .header("Authorization", nurse))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_011_invalidTimesAndInvalidMonthReturn422RatherThan500() throws Exception {
        mvc.perform(command(
                        post("/api/v1/shift-templates"),
                        "{\"name\":\"X\",\"startTime\":\"29:00\",\"endTime\":\"15:00\",\"scopeType\":\"FACILITY\"}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(command(post("/api/v1/shift-schedules"), "{\"month\":\"2038-13\",\"scopeType\":\"FACILITY\"}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
