package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static com.eldercare.backend.administration.account.PermissionCommands.restore;
import static com.eldercare.backend.administration.account.PermissionCommands.restrict;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T074 (US7 #1, #3 → #5; FR-018, FR-020, FR-025, FR-012; BR-M15-18): Quản lý viện chỉ thu hẹp trong ô ma trận (bỏ
 * thao tác, hạ toàn viện xuống theo phân công), vượt ma trận thì 409 {@code EXCEEDS_MAXIMUM_PERMISSION}; thu hẹp theo
 * tài khoản không ảnh hưởng tài khoản khác cùng vai trò; khôi phục về đúng mức ma trận; không làm viện mất Quản lý viện
 * có quyền phân quyền. Tasks ghi {@code ThuHepKhongVuotIT}.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class RestrictWithinMatrixIT extends MySqlIntegrationTestBase {

    private static final String COST_CREATE = "$[?(@.function == 'COST_ADJUSTMENT' && @.operation == 'CREATE')]";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    AccountFixtures fixtures;
    PermissionCommands commands;
    String manager;

    @BeforeEach
    void managerSignedIn() {
        fixtures = new AccountFixtures(jdbc, time);
        commands = new PermissionCommands(mvc, jdbc);
        manager = fixtures.session(fixtures.staffAccount("AC-01"));
    }

    @AfterEach
    void closeRoleRestrictions() {
        commands.closeRoleRestrictions();
    }

    @Test
    void US7_1_rolePermissions_matchMatrix_etagIsRoleVersion() throws Exception {
        int rows = jdbc.queryForObject("SELECT COUNT(*) FROM permission_matrix WHERE role = 'AC-05'", Integer.class);

        mvc.perform(get("/api/v1/roles").header("Authorization", bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'AC-04')].executionScope").value("ASSIGNED"));
        mvc.perform(get("/api/v1/roles/AC-05/permissions").header("Authorization", bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", commands.roleVersion("AC-05")))
                .andExpect(jsonPath("$.role").value("AC-05"))
                .andExpect(jsonPath("$.maximum.length()").value(rows))
                .andExpect(jsonPath("$.current.length()").value(rows))
                .andExpect(jsonPath("$.restrictions").isEmpty());
    }

    @Test
    void US7_3_beyondMatrix_byRoleOrAccount_409() throws Exception {
        long careStaff = fixtures.staffAccount("AC-05");

        commands.role(
                        manager,
                        "AC-05",
                        "restrict",
                        commands.roleVersion("AC-05"),
                        restrict("PRESCRIPTION", "VIEW", "REMOVE_OPERATION"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EXCEEDS_MAXIMUM_PERMISSION"));
        commands.account(manager, careStaff, "restrict", "\"0\"", restrict("PRESCRIPTION", "VIEW", "REMOVE_OPERATION"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EXCEEDS_MAXIMUM_PERMISSION"));
        // Hạ phạm vi của ô đã theo phân công: không còn gì để hạ
        commands.account(manager, careStaff, "restrict", "\"0\"", restrict("CARE_PLAN", "VIEW", "LOWER_SCOPE"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM permission_assignment WHERE kind = 'RESTRICTION' AND account_id = ?",
                        Integer.class,
                        careStaff))
                .isZero();
    }

    @Test
    void US7_4_US7_5_accountRestriction_otherAccountsUnaffected_thenRestored() throws Exception {
        long office = fixtures.staffAccount("AC-09");
        long otherOffice = fixtures.staffAccount("AC-09");
        String officeToken = fixtures.session(office);

        String body = commands.account(
                        manager, office, "restrict", "\"0\"", restrict("COST_ADJUSTMENT", "CREATE", "REMOVE_OPERATION"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.accountRestrictions[0].function").value("COST_ADJUSTMENT"))
                .andExpect(jsonPath("$.current" + COST_CREATE.substring(1)).isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long restrictionId = ((Number) JsonPath.read(body, "$.accountRestrictions[0].id")).longValue();

        mvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(officeToken)))
                .andExpect(jsonPath(COST_CREATE).isEmpty());
        mvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(fixtures.session(otherOffice))))
                .andExpect(jsonPath(COST_CREATE).isNotEmpty());

        commands.account(manager, office, "restore-permission", "\"1\"", restore(restrictionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountRestrictions").isEmpty());
        mvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(officeToken)))
                .andExpect(jsonPath(COST_CREATE).isNotEmpty());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'PERMISSION_RESTORED' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(office)))
                .isEqualTo(1);
    }

    @Test
    void BR_M15_18_roleRestriction_appliesToRole_otherRoleKeepsPermission() throws Exception {
        long office = fixtures.staffAccount("AC-09");
        long officeAndAccountant = fixtures.staffAccount("AC-09", "AC-14");

        String body = commands.role(
                        manager,
                        "AC-09",
                        "restrict",
                        commands.roleVersion("AC-09"),
                        restrict("COST_ADJUSTMENT", "VIEW", "REMOVE_OPERATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restrictions[0].function").value("COST_ADJUSTMENT"))
                .andExpect(jsonPath("$.current[?(@.function == 'COST_ADJUSTMENT' && @.operation == 'VIEW')]")
                        .isEmpty())
                .andExpect(jsonPath("$.maximum[?(@.function == 'COST_ADJUSTMENT' && @.operation == 'VIEW')]")
                        .isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String costView = "$[?(@.function == 'COST_ADJUSTMENT' && @.operation == 'VIEW')]";

        mvc.perform(get("/api/v1/me/permissions").header("Authorization", bearer(fixtures.session(office))))
                .andExpect(jsonPath(costView).isEmpty());
        mvc.perform(get("/api/v1/me/permissions")
                        .header("Authorization", bearer(fixtures.session(officeAndAccountant))))
                .andExpect(jsonPath(costView).isNotEmpty());

        long restrictionId = ((Number) JsonPath.read(body, "$.restrictions[0].id")).longValue();
        commands.role(manager, "AC-09", "restore-permission", commands.roleVersion("AC-09"), restore(restrictionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restrictions").isEmpty());
    }

    @Test
    void FR_012_roleRestrictionLeavingNoManagerWithPermissionControl_409() throws Exception {
        commands.role(
                        manager,
                        "AC-01",
                        "restrict",
                        commands.roleVersion("AC-01"),
                        restrict("ACCOUNT_PERMISSION_PARAMETER", "UPDATE", "REMOVE_OPERATION"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NO_MANAGER_LEFT"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM permission_assignment WHERE kind = 'RESTRICTION' AND role = 'AC-01'"
                                + " AND effective_to IS NULL",
                        Integer.class))
                .isZero();
    }
}
