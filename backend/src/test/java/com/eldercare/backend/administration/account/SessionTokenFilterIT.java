package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.internal.auth.service.SessionTokens;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T011 (research A-02, A-15; FR-013, FR-013a, BR-M15-21): token phiên opaque qua cookie {@code session} (web) hoặc
 * {@code Authorization: Bearer} (app); tra phiên theo khóa chính mỗi yêu cầu; quá CFG-M15-10 của kênh thì kết thúc phiên
 * {@code TIMED_OUT} và 401 {@code SESSION_EXPIRED}; lệnh ghi qua cookie phải có {@code X-Requested-From: web}; ba đường
 * không cần phiên. Đường {@code /api/v1/khong-co} không có controller: qua được bộ lọc thì 404, không qua thì 401.
 */
@AutoConfigureMockMvc
class SessionTokenFilterIT extends MySqlIntegrationTestBase {

    private static final String PROBE = "/api/v1/khong-co";
    private static final AtomicLong STAFF_IDS = new AtomicLong(900_000);

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void BR_73_webIdleBeyondCfgM15_10_endsSessionTimedOut() throws Exception {
        Session s = openSession("WEB");
        clock.advanceBy(Duration.ofMinutes(31));

        mvc.perform(get(PROBE).cookie(new Cookie("session", s.token())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SESSION_EXPIRED"));

        assertThat(jdbc.queryForObject(
                        "SELECT end_reason FROM login_session WHERE token_hash = ?",
                        String.class,
                        SessionTokens.hash(s.token())))
                .isEqualTo("TIMED_OUT");
        assertThat(jdbc.queryForObject("SELECT status FROM account WHERE id = ?", String.class, s.accountId()))
                .isEqualTo("ACTIVE");
    }

    private Session openSession(String channel) {
        long staffId = STAFF_IDS.incrementAndGet();
        LocalDateTime now = utc(time.now());
        jdbc.update("""
                INSERT INTO account (username, type, staff_id, status, password_hash, must_change_password,
                                     inactivity_reference_at, created_at)
                VALUES (?, 'STAFF', ?, 'ACTIVE', 'x', FALSE, ?, ?)
                """, "session-it-" + staffId, staffId, now, now);
        long accountId = jdbc.queryForObject("SELECT id FROM account WHERE staff_id = ?", Long.class, staffId);
        String token = SessionTokens.generate();
        String deviceId = UUID.randomUUID().toString();
        jdbc.update("""
                INSERT INTO login_session (token_hash, account_id, device_id, channel, started_at, last_active_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, SessionTokens.hash(token), accountId, deviceId, channel, now, now);
        return new Session(token, accountId, deviceId);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record Session(String token, long accountId, String deviceId) {}
}
