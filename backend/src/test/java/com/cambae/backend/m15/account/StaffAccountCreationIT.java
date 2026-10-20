package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.StaffStatus;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import com.cambae.backend.support.fake.m15.FakeStaffProfiles;
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
 * 002 T027 (US2 #1 → #3; FR-002 → FR-004a, FR-006): Quản lý viện tạo tài khoản nhân viên cho hồ sơ Đang làm việc có
 * chức danh dùng hệ thống, ít nhất một vai trò nhân viên; mỗi hồ sơ tối đa một tài khoản; tài khoản Hoạt động, buộc đổi
 * mật khẩu, mật khẩu tạm trả một lần, nhật ký ghi lần tạo. Chỉ Quản lý viện (Hành chính chỉ với tài khoản người thân,
 * chú thích ¹²).
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class StaffAccountCreationIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeStaffProfiles staffProfiles;

    AccountFixtures fixtures;
    String manager;

    @BeforeEach
    void managerSignedIn() {
        fixtures = new AccountFixtures(jdbc, time);
        manager = fixtures.session(fixtures.staffAccount("AC-01"));
    }

    @Test
    void US2_1_workingStaffWithSystemPosition_createdActive_mustChangePassword_audited() throws Exception {
        long staffId = AccountFixtures.nextId();
        staffProfiles.staff(staffId, Role.NURSING_STAFF);

        String username = "dieu-duong-" + staffId;
        create(manager, staffId, username, "AC-04")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.type").value("STAFF"))
                .andExpect(jsonPath("$.staffId").value(staffId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("AC-04"))
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty());

        long accountId = jdbc.queryForObject("SELECT id FROM account WHERE staff_id = ?", Long.class, staffId);
        assertThat(jdbc.queryForObject(
                        "SELECT must_change_password FROM account WHERE id = ?", Boolean.class, accountId))
                .isTrue();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'ACCOUNT_CREATED' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(accountId)))
                .isEqualTo(1);
    }

    @Test
    void US2_2_profileAlreadyHasAccount_409() throws Exception {
        long staffId = AccountFixtures.nextId();
        staffProfiles.staff(staffId, Role.CAREGIVER);
        create(manager, staffId, "cham-soc-" + staffId, "AC-05").andExpect(status().isCreated());

        create(manager, staffId, "cham-soc-2-" + staffId, "AC-05")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PROFILE_ALREADY_HAS_ACCOUNT"));
    }

    @Test
    void FR_004_resignedProfile_409() throws Exception {
        long staffId = AccountFixtures.nextId();
        staffProfiles.staff(staffId, Role.CAREGIVER).status(staffId, StaffStatus.RESIGNED);

        create(manager, staffId, "nghi-" + staffId, "AC-05")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STAFF_RESIGNED"));
    }

    @Test
    void FR_004a_positionNotSystemUser_409() throws Exception {
        long staffId = AccountFixtures.nextId();

        create(manager, staffId, "bao-ve-" + staffId, "AC-05")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("POSITION_NOT_SYSTEM_USER"));
    }

    @Test
    void US2_3_noRoleOrFamilyRole_rejected() throws Exception {
        long staffId = AccountFixtures.nextId();
        staffProfiles.staff(staffId, Role.CAREGIVER);

        create(manager, staffId, "khong-vai-tro-" + staffId).andExpect(status().is4xxClientError());
        create(manager, staffId, "nguoi-than-" + staffId, "AC-10").andExpect(status().is4xxClientError());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM account WHERE staff_id = ?", Integer.class, staffId))
                .isZero();
    }

    @Test
    void FR_004_onlyManager_officeAndFloorManagerDenied() throws Exception {
        long staffId = AccountFixtures.nextId();
        staffProfiles.staff(staffId, Role.CAREGIVER);
        String office = fixtures.session(fixtures.staffAccount("AC-09"));
        String floorManager = fixtures.session(fixtures.staffAccount("AC-02"));

        create(office, staffId, "hc-tao-" + staffId, "AC-05").andExpect(status().isForbidden());
        create(floorManager, staffId, "tt-tao-" + staffId, "AC-05").andExpect(status().isForbidden());
    }

    private ResultActions create(String token, long staffId, String username, String... roles) throws Exception {
        String roleJson = roles.length == 0 ? "" : "\"" + String.join("\",\"", roles) + "\"";
        return mvc.perform(post("/api/v1/accounts")
                .header("Authorization", bearer(token))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"staffId\":" + staffId + ",\"username\":\"" + username + "\",\"roles\":[" + roleJson
                        + "]}"));
    }
}
