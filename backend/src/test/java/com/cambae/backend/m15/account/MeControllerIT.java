package com.cambae.backend.m15.account;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.auth.internal.SessionTokens;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T025 (FR-042; contracts/api-002.yaml {@code /me}, {@code /me/permissions}): người dùng của phiên và tập quyền hiện
 * hành để client ẩn hiện chức năng; quyền vướng chú thích nhiệm vụ không có ở đây (đến từ lớp nhiệm vụ, US4).
 */
@AutoConfigureMockMvc
class MeControllerIT extends MySqlIntegrationTestBase {

    private static final AtomicLong IDS = new AtomicLong(980_000);

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Test
    void FR_042_me_returnsSessionUser() throws Exception {
        Session nurse = signIn("AC-04");

        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + nurse.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(nurse.accountId()))
                .andExpect(jsonPath("$.type").value("STAFF"))
                .andExpect(jsonPath("$.roles[0]").value("AC-04"))
                .andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    @Test
    void FR_042_permissions_listsEffectiveRolePermissions() throws Exception {
        Session nurse = signIn("AC-04");

        mvc.perform(get("/api/v1/me/permissions").header("Authorization", "Bearer " + nurse.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.function == 'RESIDENT_PROFILE' && @.operation == 'VIEW')].scope")
                        .value("ASSIGNED"))
                .andExpect(jsonPath("$[?(@.function == 'RESIDENT_PROFILE' && @.operation == 'VIEW')].source")
                        .value("ROLE"))
                .andExpect(jsonPath("$[*].function", not(hasItem("BED_READINESS"))));
    }

    @Test
    void FR_013_withoutSession_401() throws Exception {
        mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    private Session signIn(String role) {
        long staffId = IDS.incrementAndGet();
        LocalDateTime now = LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC);
        LocalDateTime created = LocalDateTime.ofInstant(time.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
        jdbc.update("""
                INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'STAFF', ?, 'ACTIVE', 'x', TRUE, ?, ?)
                """, "me-it-" + staffId, staffId, created, created);
        long accountId = jdbc.queryForObject("SELECT id FROM account WHERE staff_id = ?", Long.class, staffId);
        jdbc.update(
                "INSERT INTO role_assignment (account_id, role, effective_from, reason) VALUES (?, ?, ?, 'Test')",
                accountId,
                role,
                created);
        String token = SessionTokens.generate();
        jdbc.update("""
                INSERT INTO login_session (token_hash, account_id, device_id, channel, started_at, last_active_at)
                VALUES (?, ?, ?, 'STAFF_APP', ?, ?)
                """, SessionTokens.hash(token), accountId, UUID.randomUUID().toString(), now, now);
        return new Session(token, accountId);
    }

    private record Session(String token, long accountId) {}
}
