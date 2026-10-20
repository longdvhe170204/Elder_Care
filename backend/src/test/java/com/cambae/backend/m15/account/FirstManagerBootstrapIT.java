package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.ConditionNotMetException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.internal.auth.service.FirstManagerBootstrap;
import com.cambae.backend.m15.account.internal.auth.service.FirstManagerBootstrap.Result;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import com.cambae.backend.support.fake.m15.FakeStaffProfiles;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 002 T016 (BR-M15-22, FR-004b, Q-310; research A-03): công cụ khởi tạo tạo tài khoản Quản lý viện đầu tiên cùng hồ sơ
 * nhân viên tối thiểu (008), chỉ khi chưa có tài khoản nào đang có vai trò AC-01; mật khẩu tạm đạt CFG-M15-09, băm
 * Argon2id, buộc đổi ở lần đăng nhập đầu; nhật ký ghi người thực hiện "khởi tạo hệ thống". Mỗi test chạy trong
 * transaction rồi rollback: đóng tạm các dòng AC-01 của test khác để có trạng thái "chưa có Quản lý viện".
 */
@Transactional
@Import(FakeM15Config.class)
class FirstManagerBootstrapIT extends MySqlIntegrationTestBase {

    @Autowired
    FirstManagerBootstrap bootstrap;

    @Autowired
    FakeStaffProfiles staffProfiles;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @BeforeEach
    void noManagerYet() {
        jdbc.update(
                "UPDATE role_assignment SET effective_to = ? WHERE role = 'AC-01' AND effective_to IS NULL",
                LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC));
    }

    @Test
    void BR_M15_22_noManager_createsActiveManagerThatMustChangePassword() {
        Result result = bootstrap.run("quanly", "Nguyễn Văn Quản");

        Map<String, Object> account = jdbc.queryForMap("SELECT * FROM account WHERE id = ?", result.accountId());
        assertThat(account)
                .containsEntry("username", "quanly")
                .containsEntry("type", "STAFF")
                .containsEntry("status", "ACTIVE")
                .containsEntry("must_change_password", true)
                .containsEntry("created_by", null);
        long staffId = ((Number) account.get("staff_id")).longValue();
        assertThat(staffProfiles.createdMinimal()).containsEntry(staffId, "Nguyễn Văn Quản");
        assertThat(staffProfiles.positionRole(staffId)).contains(Role.NURSING_HOME_MANAGER);
        assertThat(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
                        .matches(result.temporaryPassword(), (String) account.get("password_hash")))
                .isTrue();
        assertThat(jdbc.queryForList(
                        "SELECT role FROM role_assignment WHERE account_id = ? AND effective_to IS NULL"
                                + " AND assigned_by IS NULL",
                        String.class,
                        result.accountId()))
                .containsExactly("AC-01");
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM account_status_history WHERE account_id = ? AND actor_id IS NULL",
                        String.class,
                        result.accountId()))
                .isEqualTo("ACTIVE");
    }

    @Test
    void BR_M15_22_auditLog_actorIsSystemBootstrap_withRuleBasis() {
        Result result = bootstrap.run("quanly", "Nguyễn Văn Quản");

        List<Map<String, Object>> logs = jdbc.queryForList(
                "SELECT action, actor_type, actor_id, basis FROM audit_log WHERE entity_type = 'M15.ACCOUNT'"
                        + " AND entity_id = ? ORDER BY id",
                String.valueOf(result.accountId()));
        assertThat(logs).extracting(r -> r.get("action")).containsExactly("ACCOUNT_CREATED", "ROLE_ASSIGNED");
        assertThat(logs).allSatisfy(r -> {
            assertThat(r.get("actor_type")).isEqualTo("BOOTSTRAP");
            assertThat(r.get("actor_id")).isNull();
            assertThat((String) r.get("basis")).contains("BR-M15-22");
        });
    }

    @Test
    void CFG_M15_09_temporaryPassword_atLeast8CharsWithLettersAndDigits() {
        String password = bootstrap.run("quanly", "Nguyễn Văn Quản").temporaryPassword();

        assertThat(password)
                .hasSizeGreaterThanOrEqualTo(8)
                .containsPattern("[A-Za-z]")
                .containsPattern("[0-9]");
    }

    @Test
    void BR_M15_22_managerAlreadyExists_refused_noSecondAccount() {
        bootstrap.run("quanly", "Nguyễn Văn Quản");
        int accounts = jdbc.queryForObject("SELECT COUNT(*) FROM account", Integer.class);

        assertThatThrownBy(() -> bootstrap.run("quanly2", "Trần Thị Hai"))
                .isInstanceOfSatisfying(
                        ConditionNotMetException.class,
                        e -> assertThat(e.unmetConditions())
                                .extracting(c -> c.code())
                                .containsExactly("BR-M15-22"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM account", Integer.class))
                .isEqualTo(accounts);
    }
}
