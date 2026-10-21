package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.PermissionCommands.restrict;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T074 (FR-026, BR-M15-17, CHK034): hai Quản lý viện cùng đổi quyền; lần lưu theo {@code If-Match} cũ bị 412 kèm
 * tập quyền hiện hành ({@code currentValue}), cho tài khoản ({@code account.permission_version}) và cho vai trò
 * ({@code role.permission_version}). Tasks ghi {@code DoiQuyenDongThoiIT}.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class ConcurrentPermissionChangeIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    AccountFixtures fixtures;
    PermissionCommands commands;
    String manager;
    String otherManager;

    @BeforeEach
    void twoManagers() {
        fixtures = new AccountFixtures(jdbc, time);
        commands = new PermissionCommands(mvc, jdbc);
        manager = fixtures.session(fixtures.staffAccount("AC-01"));
        otherManager = fixtures.session(fixtures.staffAccount("AC-01"));
    }

    @AfterEach
    void closeRoleRestrictions() {
        commands.closeRoleRestrictions();
    }

    @Test
    void BR_M15_17_account_staleIfMatch_412_withCurrentPermissions() throws Exception {
        long office = fixtures.staffAccount("AC-09");
        String seen = commands.accountVersion(office);

        commands.account(manager, office, "restrict", seen, restrict("COST_ADJUSTMENT", "CREATE", "REMOVE_OPERATION"))
                .andExpect(status().isOk());
        commands.account(
                        otherManager,
                        office,
                        "restrict",
                        seen,
                        restrict("COST_ADJUSTMENT", "UPDATE", "REMOVE_OPERATION"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.currentValue.accountId").value(office))
                .andExpect(jsonPath("$.currentValue.accountRestrictions[0].operation")
                        .value("CREATE"));
    }

    @Test
    void BR_M15_17_role_staleIfMatch_412_withCurrentRolePermissions() throws Exception {
        String seen = commands.roleVersion("AC-14");

        commands.role(manager, "AC-14", "restrict", seen, restrict("COST_ADJUSTMENT", "VIEW", "LOWER_SCOPE"))
                .andExpect(status().isOk());
        commands.role(otherManager, "AC-14", "restrict", seen, restrict("COST_ADJUSTMENT", "VIEW", "REMOVE_OPERATION"))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.currentValue.role").value("AC-14"))
                .andExpect(jsonPath("$.currentValue.restrictions[0].kind").value("LOWER_SCOPE"));
    }

    @Test
    void BR_M15_17_grant_staleIfMatch_412() throws Exception {
        long nurse = fixtures.staffAccount("AC-04");

        commands.account(manager, nurse, "grant-care-plan-approval", "\"0\"", PermissionCommands.REASON)
                .andExpect(status().isOk());
        commands.account(otherManager, nurse, "revoke-care-plan-approval", "\"0\"", PermissionCommands.REASON)
                .andExpect(status().isPreconditionFailed());
    }
}
