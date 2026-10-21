package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 002 T089 (US8 #9; FR-075): tài khoản người thân (AC-10) gọi endpoint tài khoản, thiết bị, nhật ký của tài khoản người
 * cao tuổi thì 403 hoặc 404 (dòng "Tài khoản người cao tuổi" của Phụ lục 27 không có quyền cho người thân). Tasks ghi
 * {@code NguoiThanKhongXemTaiKhoanNctIT}.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class FamilyCannotSeeResidentAccountIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    AccountFixtures fixtures;
    ResidentLogins logins;
    String family;
    long account;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
        logins = new ResidentLogins(mvc);
        family = fixtures.session(fixtures.familyAccount(AccountFixtures.nextId()));
        account = fixtures.residentAccount(AccountFixtures.nextId());
    }

    @Test
    void FR_075_familyMember_refusedOnResidentAccountEndpoints() throws Exception {
        List<ResultActions> calls = List.of(
                mvc.perform(get("/api/v1/accounts/" + account).header("Authorization", bearer(family))),
                mvc.perform(
                        get("/api/v1/accounts/" + account + "/permissions").header("Authorization", bearer(family))),
                logins.createCode(family, account),
                logins.command(family, account, "unbind-device", "\"0\"", "{\"reason\":\"Thử\"}"),
                logins.command(
                        family, account, "lock-resident", "\"0\"", "{\"basis\":\"WITH_REASON\",\"reason\":\"Thử\"}"),
                logins.command(
                        family,
                        account,
                        "unlock-resident",
                        "\"0\"",
                        "{\"consent\":{\"method\":\"REQUESTED\",\"withStaffId\":1}}"),
                mvc.perform(get("/api/v1/audit-logs/entities/M15.ACCOUNT/" + account)
                        .header("Authorization", bearer(family))));

        for (ResultActions call : calls) {
            assertThat(call.andReturn().getResponse().getStatus()).isIn(403, 404);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM login_qr_code WHERE account_id = ?", Integer.class, account))
                .isZero();
    }
}
