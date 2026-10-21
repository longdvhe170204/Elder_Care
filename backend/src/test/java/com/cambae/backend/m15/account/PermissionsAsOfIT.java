package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static com.cambae.backend.m15.account.PermissionCommands.restore;
import static com.cambae.backend.m15.account.PermissionCommands.restrict;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 002 T074 (FR-026a, BR-M15-16; T076 phần {@code asOf}): Quản lý viện tra tập quyền của một tài khoản tại thời điểm quá
 * khứ, dựng từ lịch sử gán vai trò, thu hẹp, quyền gán thêm (đồng hồ giả lập); không tra thời điểm tương lai (FR-027).
 * Tasks ghi {@code QuyenTaiThoiDiemIT}.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class PermissionsAsOfIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    AccountFixtures fixtures;
    PermissionCommands commands;
    long managerAccount;

    @BeforeEach
    void setUp() {
        clock.reset();
        fixtures = new AccountFixtures(jdbc, time);
        commands = new PermissionCommands(mvc, jdbc);
        managerAccount = fixtures.staffAccount("AC-01");
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void FR_026a_permissionsAtPastInstants_fromHistory() throws Exception {
        Map<String, Object> first = jdbc.queryForMap("SELECT function_code, operation FROM permission_matrix"
                + " WHERE role = 'AC-05' ORDER BY function_code, operation LIMIT 1");
        String function = (String) first.get("function_code");
        String operation = (String) first.get("operation");
        String key = "$.current[?(@.function == '" + function + "' && @.operation == '" + operation + "')]";
        long account = fixtures.staffAccount("AC-05");
        Instant start = time.now();

        clock.advanceBy(Duration.ofHours(1));
        String body = commands.account(
                        manager(), account, "restrict", "\"0\"", restrict(function, operation, "REMOVE_OPERATION"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long restrictionId = ((Number) JsonPath.read(body, "$.accountRestrictions[0].id")).longValue();
        clock.advanceBy(Duration.ofHours(1));
        commands.account(manager(), account, "assign-role", "\"1\"", "{\"role\":\"AC-04\",\"reason\":\"Có chứng chỉ\"}")
                .andExpect(status().isOk());
        clock.advanceBy(Duration.ofHours(1));
        commands.account(manager(), account, "restore-permission", "\"2\"", restore(restrictionId))
                .andExpect(status().isOk());

        asOf(account, start.plus(Duration.ofMinutes(30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf")
                        .value(start.plus(Duration.ofMinutes(30)).toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("AC-05")))
                .andExpect(jsonPath("$.accountRestrictions").isEmpty())
                .andExpect(jsonPath(key).isNotEmpty());
        asOf(account, start.plus(Duration.ofMinutes(90)))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("AC-05")))
                .andExpect(jsonPath("$.accountRestrictions[0].id").value(restrictionId))
                .andExpect(jsonPath(key).isEmpty());
        asOf(account, start.plus(Duration.ofMinutes(150)))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("AC-04", "AC-05")))
                .andExpect(jsonPath("$.accountRestrictions[0].id").value(restrictionId));
        asOf(account, time.now())
                .andExpect(jsonPath("$.roles", containsInAnyOrder("AC-04", "AC-05")))
                .andExpect(jsonPath("$.accountRestrictions").isEmpty())
                .andExpect(jsonPath(key).isNotEmpty());
    }

    @Test
    void FR_026a_statusAtPastInstant() throws Exception {
        long account = fixtures.staffAccount("AC-05");
        Instant before = time.now();
        clock.advanceBy(Duration.ofMinutes(10));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/v1/accounts/" + account + "/lock")
                        .header("Authorization", bearer(manager()))
                        .header("If-Match", "\"0\"")
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Tạm ngừng làm việc\"}"))
                .andExpect(status().isOk());

        asOf(account, before).andExpect(jsonPath("$.status").value("ACTIVE"));
        asOf(account, time.now()).andExpect(jsonPath("$.status").value("LOCKED"));
    }

    @Test
    void FR_027_futureInstant_422() throws Exception {
        long account = fixtures.staffAccount("AC-05");

        asOf(account, time.now().plus(Duration.ofDays(1))).andExpect(status().isUnprocessableEntity());
    }

    /** Phiên mới sau mỗi lần tua để không vượt thời gian tự đăng xuất (CFG-M15-10). */
    private String manager() {
        return fixtures.session(managerAccount);
    }

    private ResultActions asOf(long account, Instant at) throws Exception {
        return mvc.perform(get("/api/v1/accounts/" + account + "/permissions")
                .param("asOf", at.toString())
                .header("Authorization", bearer(manager())));
    }
}
