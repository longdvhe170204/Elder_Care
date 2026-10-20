package com.cambae.backend.m15.account;

import static com.cambae.backend.m15.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.scheduling.ScheduledJob;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.auditlog.api.Basis;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeFamilyRelationships;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import com.cambae.backend.support.fake.m15.FakeResidencyEnds;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 002 T068 (US6 #5, #6; BR-M15-14, FR-049, FR-011): lệnh chấm dứt quan hệ của 012 gọi
 * {@code AccountApi.recheckFamilyMember} trong transaction của mình; không còn quan hệ Hiệu lực thì Đã khóa nguyên nhân
 * hết quan hệ, phiên kết thúc, nhật ký Bộ lập lịch kèm căn cứ. Job khóa sau kết thúc lưu trú: mọi người cao tuổi có quan
 * hệ đã quá CFG-M01-04 từ thời điểm muộn nhất thì Đã khóa nguyên nhân hết hạn; mở khóa bị chặn khi nguyên nhân còn.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class RelationshipEndLockIT extends MySqlIntegrationTestBase {

    private static final Basis RELATIONSHIP_ENDED = new Basis("BR-M15-14", null, null, null);

    @Autowired
    AccountApi accounts;

    @Autowired
    List<ScheduledJob> jobs;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeFamilyRelationships relationships;

    @Autowired
    FakeResidencyEnds residencyEnds;

    AccountFixtures fixtures;
    long familyMember;
    long account;

    @BeforeEach
    void setUp() {
        fixtures = new AccountFixtures(jdbc, time);
        familyMember = AccountFixtures.nextId();
        account = fixtures.familyAccount(familyMember);
    }

    @Test
    void BR_M15_14_lastRelationshipEnded_locked_sessionsEnded_auditedAsScheduler() throws Exception {
        long relationship = relationships.relate(familyMember, AccountFixtures.nextId());
        String token = fixtures.session(account);

        tx.executeWithoutResult(s -> {
            relationships.end(relationship);
            accounts.recheckFamilyMember(familyMember, RELATIONSHIP_ENDED);
        });

        assertThat(jdbc.queryForMap("SELECT status, lock_cause FROM account WHERE id = ?", account))
                .containsEntry("status", "LOCKED")
                .containsEntry("lock_cause", "RELATIONSHIP_ENDED");
        mvc.perform(get("/api/v1/me").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
        Map<String, Object> log = jdbc.queryForMap(
                "SELECT actor_type, basis FROM audit_log WHERE action = 'ACCOUNT_LOCKED' AND entity_id = ?",
                String.valueOf(account));
        assertThat(log.get("actor_type")).isEqualTo("SCHEDULER");
        assertThat((String) log.get("basis")).contains("BR-M15-14");
    }

    @Test
    void FR_049_otherRelationshipRemains_staysActive() {
        long ending = relationships.relate(familyMember, AccountFixtures.nextId());
        relationships.relate(familyMember, AccountFixtures.nextId());

        tx.executeWithoutResult(s -> {
            relationships.end(ending);
            accounts.recheckFamilyMember(familyMember, RELATIONSHIP_ENDED);
        });

        assertThat(accountStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void FR_049_mustRunInsideRelationshipTransaction() {
        assertThatThrownBy(() -> accounts.recheckFamilyMember(familyMember, RELATIONSHIP_ENDED))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void US6_5_allResidentsEndedPastGraceFromLatest_jobLocks() {
        long first = AccountFixtures.nextId();
        long second = AccountFixtures.nextId();
        relationships.relate(familyMember, first);
        relationships.relate(familyMember, second);
        residencyEnds.ended(first, time.now().minus(Duration.ofDays(40)));
        residencyEnds.ended(second, time.now().minus(Duration.ofDays(29)));

        job().runNow();
        assertThat(accountStatus()).isEqualTo("ACTIVE");

        residencyEnds.ended(second, time.now().minus(Duration.ofDays(31)));
        job().runNow();
        job().runNow();

        assertThat(jdbc.queryForObject("SELECT lock_cause FROM account WHERE id = ?", String.class, account))
                .isEqualTo("EXPIRED_AFTER_RESIDENCY_END");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'ACCOUNT_LOCKED' AND entity_id = ?"
                                + " AND actor_type = 'SCHEDULER' AND basis LIKE '%CFG-M01-04%'",
                        Integer.class, String.valueOf(account)))
                .isEqualTo(1);
    }

    @Test
    void US6_6_oneResidentStillStaying_jobKeepsActive() {
        long ended = AccountFixtures.nextId();
        relationships.relate(familyMember, ended);
        relationships.relate(familyMember, AccountFixtures.nextId());
        residencyEnds.ended(ended, time.now().minus(Duration.ofDays(31)));

        job().runNow();

        assertThat(accountStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void FR_011_unlockRefusedWhileCauseRemains() throws Exception {
        long relationship = relationships.relate(familyMember, AccountFixtures.nextId());
        tx.executeWithoutResult(s -> {
            relationships.end(relationship);
            accounts.recheckFamilyMember(familyMember, RELATIONSHIP_ENDED);
        });
        String manager = fixtures.session(fixtures.staffAccount("AC-01"));

        unlock(manager)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("LOCK_CAUSE_STILL_EXISTS"));

        relationships.relate(familyMember, AccountFixtures.nextId());
        unlock(manager).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions unlock(String token) throws Exception {
        String version = jdbc.queryForObject("SELECT version FROM account WHERE id = ?", String.class, account);
        return mvc.perform(post("/api/v1/accounts/" + account + "/unlock")
                .header("Authorization", bearer(token))
                .header("If-Match", "\"" + version + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Người thân có quan hệ mới\"}"));
    }

    private String accountStatus() {
        return jdbc.queryForObject("SELECT status FROM account WHERE id = ?", String.class, account);
    }

    private ScheduledJob job() {
        return jobs.stream()
                .filter(j -> j.name().equals("LOCK_AFTER_RESIDENCY_END"))
                .findFirst()
                .orElseThrow();
    }
}
