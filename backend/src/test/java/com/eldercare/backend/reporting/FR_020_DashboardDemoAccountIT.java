package com.eldercare.backend.reporting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Dashboard với tài khoản mẫu của profile demo và bean thật (không bản giả), như khi chạy {@code demo}: trước đây Điều
 * dưỡng nhận 401 thay vì dashboard (FR-020).
 */
@AutoConfigureMockMvc
class FR_020_DashboardDemoAccountIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Test
    void FR_020_demoNurse_getsDashboard() throws Exception {
        long accountId = jdbc.queryForObject("SELECT id FROM account WHERE username = 'demo.dieuduong'", Long.class);
        String token = new TestUsers(jdbc, time).session(accountId);

        mvc.perform(get("/api/v1/dashboard").header("Authorization", TestUsers.bearer(token)))
                .andExpect(status().isOk());
    }
}
