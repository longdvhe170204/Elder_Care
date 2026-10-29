package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * 002 T029 (US2 #9; FR-012): luôn còn ít nhất một tài khoản Hoạt động có vai trò Quản lý viện; khóa hoặc gỡ vai trò
 * Quản lý viện của người cuối cùng bị từ chối 409 {@code NO_MANAGER_LEFT}. Chạy trong transaction rồi rollback: đóng tạm
 * các dòng AC-01 của test khác để viện chỉ còn những Quản lý viện do test tạo.
 */
@Transactional
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class ManagerAlwaysExistsIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    AccountFixtures fixtures;

    @BeforeEach
    void onlyManagersOfThisTest() {
        jdbc.update(
                "UPDATE role_assignment SET effective_to = ? WHERE role = 'AC-01' AND effective_to IS NULL",
                LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC));
        fixtures = new AccountFixtures(jdbc, time);
    }

    @Test
    void FR_012_lockOrDemoteLastManager_409() throws Exception {
        long manager = fixtures.staffAccount("AC-01", "AC-03");
        String token = fixtures.session(manager);

        command(token, manager, "lock", "\"0\"", "{\"reason\":\"Thử khóa\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NO_MANAGER_LEFT"));
        command(token, manager, "remove-role", "\"0\"", "{\"role\":\"AC-01\",\"reason\":\"Thử gỡ\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NO_MANAGER_LEFT"));
    }

    @Test
    void FR_012_withAnotherActiveManager_lockAllowed() throws Exception {
        long first = fixtures.staffAccount("AC-01");
        long second = fixtures.staffAccount("AC-01");

        command(fixtures.session(first), second, "lock", "\"0\"", "{\"reason\":\"Nghỉ dài hạn\"}")
                .andExpect(status().isOk());
    }

    private ResultActions command(String token, long account, String command, String ifMatch, String body)
            throws Exception {
        return mvc.perform(post("/api/v1/accounts/" + account + "/" + command)
                .header("Authorization", bearer(token))
                .header("If-Match", ifMatch)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
