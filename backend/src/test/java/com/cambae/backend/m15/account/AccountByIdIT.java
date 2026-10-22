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
}
