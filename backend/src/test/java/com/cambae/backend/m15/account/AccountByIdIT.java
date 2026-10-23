package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 contracts mục 1.6 ("Đổi api" 2026-10-22): {@code AccountApi.byId} trả tài khoản kèm {@code staffId}, để 000 báo người
 * yêu cầu khi áp dụng không thành bằng nhóm {@code Staff} của 009 (000 FR-038).
 */
@Import(FakeM15Config.class)
class AccountByIdIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    AccountApi accounts;

    @Test
    void byId_returnsStaffId_unknownIsEmpty() {
        TestUsers.TestUser office = new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF);

        assertThat(accounts.byId(office.accountId())).hasValueSatisfying(a -> {
            assertThat(a.accountId()).isEqualTo(office.accountId());
            assertThat(a.staffId()).isEqualTo(office.staffId());
            assertThat(a.roles()).containsExactly(Role.OFFICE_STAFF);
        });
        assertThat(accounts.byId(TestUsers.nextId())).isEmpty();
    }

    @Test
    void activeWithRole_includesTemporarilyLocked_excludesLockedAndInactive() {
        // 009 FR-008: Khóa tạm vẫn nhận thông báo; Đã khóa, Không hoạt động thì không (nhóm (b) của 009)
        TestUsers users = new TestUsers(jdbc, time);
        TestUsers.TestUser active = users.staff(Role.ACCOUNTANT);
        TestUsers.TestUser tempLocked = users.staff(Role.ACCOUNTANT);
        TestUsers.TestUser locked = users.staff(Role.ACCOUNTANT);
        TestUsers.TestUser inactive = users.staff(Role.ACCOUNTANT);
        TestUsers.TestUser other = users.staff(Role.KITCHEN_STAFF);
        jdbc.update(
                "UPDATE account SET status = 'TEMPORARILY_LOCKED', locked_until = UTC_TIMESTAMP(6) + INTERVAL 1 HOUR WHERE id = ?",
                tempLocked.accountId());
        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'MANUAL' WHERE id = ?", locked.accountId());
        jdbc.update("UPDATE account SET status = 'INACTIVE' WHERE id = ?", inactive.accountId());

        assertThat(accounts.activeWithRole(Role.ACCOUNTANT))
                .extracting(a -> a.accountId())
                .contains(active.accountId(), tempLocked.accountId())
                .doesNotContain(locked.accountId(), inactive.accountId(), other.accountId());
    }
}
