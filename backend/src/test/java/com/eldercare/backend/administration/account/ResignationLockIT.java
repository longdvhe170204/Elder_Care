package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.auditlog.api.Basis;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 002 T028 (US2 #4; BR-M09-04, FR-009, FR-013): lệnh Cho nghỉ việc của 008 gọi {@code AccountApi.lockOnResignation}
 * trong transaction của mình: tài khoản Đã khóa nguyên nhân Nghỉ việc, mọi phiên kết thúc, lịch sử trạng thái thêm một
 * dòng, nhật ký ghi người thực hiện Bộ lập lịch kèm căn cứ; 008 rollback thì tài khoản không đổi; gọi ngoài transaction
 * thì lỗi ({@code MANDATORY}).
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class ResignationLockIT extends MySqlIntegrationTestBase {

    private static final Basis BR_M09_04 = new Basis("BR-M09-04", null, null, null);

    @Autowired
    AccountApi accounts;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    AccountFixtures fixtures;

    @BeforeEach
    void fixtures() {
        fixtures = new AccountFixtures(jdbc, time);
    }

    @Test
    void BR_M09_04_resignation_locksAccount_endsSessions_auditedAsScheduler() throws Exception {
        long account = fixtures.staffAccount("AC-05");
        String token = fixtures.session(account);

        tx.executeWithoutResult(s -> accounts.lockOnResignation(fixtures.staffId(account), BR_M09_04));

        assertThat(jdbc.queryForMap("SELECT status, lock_cause FROM account WHERE id = ?", account))
                .containsEntry("status", "LOCKED")
                .containsEntry("lock_cause", "RESIGNATION");
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject(
                        "SELECT end_reason FROM login_session WHERE account_id = ?", String.class, account))
                .isEqualTo("ACCOUNT_LEFT_ACTIVE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM status_history WHERE entity_type = 'M15.ACCOUNT' AND attribute = 'STATUS' AND entity_id = ? AND value = 'LOCKED'"
                                + " AND details->>'$.lockCause' = 'RESIGNATION'",
                        Integer.class,
                        account))
                .isEqualTo(1);
        Map<String, Object> log = jdbc.queryForMap(
                "SELECT actor_type, basis FROM audit_log WHERE action = 'ACCOUNT_LOCKED' AND entity_id = ?",
                String.valueOf(account));
        assertThat(log.get("actor_type")).isEqualTo("SCHEDULER");
        assertThat((String) log.get("basis")).contains("BR-M09-04");
    }

    @Test
    void FR_009_resignationRolledBack_accountUnchanged() {
        long account = fixtures.staffAccount("AC-05");

        tx.executeWithoutResult(s -> {
            accounts.lockOnResignation(fixtures.staffId(account), BR_M09_04);
            s.setRollbackOnly();
        });

        assertThat(jdbc.queryForObject("SELECT status FROM account WHERE id = ?", String.class, account))
                .isEqualTo("ACTIVE");
    }

    @Test
    void FR_009_mustRunInsideResignationTransaction() {
        long account = fixtures.staffAccount("AC-05");

        assertThatThrownBy(() -> accounts.lockOnResignation(fixtures.staffId(account), BR_M09_04))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void FR_009_staffWithoutAccount_nothingToLock() {
        tx.executeWithoutResult(s -> accounts.lockOnResignation(AccountFixtures.nextId(), BR_M09_04));
    }
}
