package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T029 (US2 #7; FR-005): không có thao tác xóa tài khoản: API không có {@code DELETE}, tài khoản ứng dụng không có
 * quyền xóa, trigger chặn cả tài khoản quản trị.
 */
@AutoConfigureMockMvc
class AccountNotDeletableIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Test
    void FR_005_noDeleteEndpoint_noDeleteGrant_triggerBlocksAdmin() throws SQLException, Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long account = fixtures.staffAccount("AC-05");
        String manager = fixtures.session(fixtures.staffAccount("AC-01"));

        mvc.perform(delete("/api/v1/accounts/" + account).header("Authorization", bearer(manager)))
                .andExpect(status().isMethodNotAllowed());
        assertThatThrownBy(() -> jdbc.update("DELETE FROM account WHERE id = ?", account))
                .isInstanceOf(DataAccessException.class)
                .rootCause()
                .hasMessageContaining("DELETE command denied");
        try (Connection c = adminConnection();
                Statement s = c.createStatement()) {
            assertThatThrownBy(() -> s.executeUpdate("DELETE FROM account WHERE id = " + account))
                    .isInstanceOf(SQLException.class)
                    .hasFieldOrPropertyWithValue("SQLState", "45000");
        }
    }
}
