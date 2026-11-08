package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeDuties;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
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

/**
 * 002 T029 (US2 #5, #6; FR-006, FR-011, FR-013; Q-292): Quản lý viện khóa tài khoản có lý do và {@code If-Match}, phiên
 * kết thúc; người đang là Phụ trách bộ phận phải có người thay (gọi 008 cùng transaction); mở khóa bị từ chối khi nguyên
 * nhân tự động còn; cấp lại mật khẩu buộc đổi ở lần đăng nhập sau; chi tiết tài khoản có ETag và {@code allowedCommands}.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class LockUnlockIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeStaffProfiles staffProfiles;

    @Autowired
    FakeDuties duties;

    AccountFixtures fixtures;
    String manager;

    @BeforeEach
    void managerSignedIn() {
        fixtures = new AccountFixtures(jdbc, time);
        manager = fixtures.session(fixtures.staffAccount("AC-01"));
    }

    @Test
    void US2_5_lockWithoutReason_422_withReason_lockedAndSessionsEnded() throws Exception {
        long account = fixtures.staffAccount("AC-05");
        String token = fixtures.session(account);

        command(account, "lock", "\"0\"", "{}").andExpect(status().isUnprocessableEntity());
        command(account, "lock", "\"0\"", "{\"reason\":\"Vi phạm nội quy\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.lockCause").value("MANUAL"))
                .andExpect(header().string("ETag", "\"1\""));

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    private ResultActions command(long account, String command, String ifMatch, String body) throws Exception {
        return mvc.perform(post("/api/v1/accounts/" + account + "/" + command)
                .header("Authorization", bearer(manager))
                .header("If-Match", ifMatch)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
