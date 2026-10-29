package com.eldercare.backend.administration.account;

import static com.eldercare.backend.administration.account.AccountFixtures.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationships;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T032a (contracts/module-interfaces.md mục 1.6, mục 2): họ tên người dùng lấy từ hồ sơ liên kết (nhân viên ở 008,
 * người thân ở 012) qua {@code AccountApi.fullName}; dùng cho {@code UserInfo.fullName} của {@code /me} và
 * {@code Actor.fullName} trong phản hồi của 000 (nhật ký, yêu cầu phê duyệt, đính chính, lịch sử tham số).
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class ActorFullNameIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FakeStaffProfiles staffProfiles;

    @Autowired
    FakeFamilyRelationships familyRelationships;

    @Autowired
    AccountApi accounts;

    @Test
    void T032a_me_hasFullNameFromStaffProfile() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long account = fixtures.staffAccount("AC-04");
        staffProfiles.name(fixtures.staffId(account), "Trần Thị Điều Dưỡng");

        mvc.perform(get("/api/v1/me").header("Authorization", bearer(fixtures.session(account))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Trần Thị Điều Dưỡng"));
        assertThat(accounts.fullName(account)).contains("Trần Thị Điều Dưỡng");
    }

    /** Schema {@code Account} có {@code fullName} (chủ dự án duyệt 2026-10-20, cho bảng tài khoản T035). */
    @Test
    void T032a_accountListAndDetail_haveFullName() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        String manager = fixtures.session(fixtures.staffAccount("AC-01"));
        long account = fixtures.staffAccount("AC-05");
        staffProfiles.name(fixtures.staffId(account), "Phạm Văn Chăm Sóc");
        String username = jdbc.queryForObject("SELECT username FROM account WHERE id = ?", String.class, account);

        mvc.perform(get("/api/v1/accounts").param("q", username).header("Authorization", bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].fullName").value("Phạm Văn Chăm Sóc"));
        mvc.perform(get("/api/v1/accounts/" + account).header("Authorization", bearer(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Phạm Văn Chăm Sóc"));
    }

    @Test
    void T032a_familyMemberAccount_hasFullNameFromFamilyProfile() {
        long familyMemberId = AccountFixtures.nextId();
        jdbc.update("""
                INSERT INTO account (username, type, family_member_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'FAMILY_MEMBER', ?, 'ACTIVE', 'x', FALSE, NOW(), NOW())
                """, "nguoi-than-" + familyMemberId, familyMemberId);
        long account =
                jdbc.queryForObject("SELECT id FROM account WHERE family_member_id = ?", Long.class, familyMemberId);
        familyRelationships.name(familyMemberId, "Lê Thị Người Thân");

        assertThat(accounts.fullName(account)).contains("Lê Thị Người Thân");
    }

    @Test
    void T032a_auditLogActor_hasFullName() throws Exception {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long manager = fixtures.staffAccount("AC-01");
        staffProfiles.name(fixtures.staffId(manager), "Nguyễn Văn Quản Lý");
        String token = fixtures.session(manager);
        long staffId = AccountFixtures.nextId();
        staffProfiles.staff(staffId, Role.CAREGIVER);
        mvc.perform(post("/api/v1/accounts")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"staffId\":" + staffId + ",\"username\":\"ho-ten-" + staffId
                                + "\",\"roles\":[\"AC-05\"]}"))
                .andExpect(status().isCreated());
        long created = jdbc.queryForObject("SELECT id FROM account WHERE staff_id = ?", Long.class, staffId);

        mvc.perform(get("/api/v1/audit-logs/entities/M15.ACCOUNT/" + created).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].actor.fullName").value("Nguyễn Văn Quản Lý"));
    }
}
