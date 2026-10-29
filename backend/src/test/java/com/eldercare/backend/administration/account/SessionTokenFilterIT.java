package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.time.temporal.ChronoUnit;
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
    void FR_013_noToken_returns401() throws Exception {
        mvc.perform(get(PROBE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void FR_013_unknownToken_returns401SessionExpired() throws Exception {
        mvc.perform(get(PROBE).header("Authorization", "Bearer " + SessionTokens.generate()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SESSION_EXPIRED"));
    }

    @Test
    void FR_013_bearerToken_passesAndTouchesSessionAndDevice() throws Exception {
        Session s = openSession("STAFF_APP");
        clock.advanceBy(Duration.ofMinutes(5));
        // Cột DATETIME(6) chỉ giữ tới micro giây nên mốc so sánh cắt cùng độ chính xác
        Instant before = time.now().truncatedTo(ChronoUnit.MICROS);

        mvc.perform(get(PROBE).header("Authorization", "Bearer " + s.token())).andExpect(status().isNotFound());

        assertThat(lastActiveAt(s)).isAfterOrEqualTo(before);
        assertThat(jdbc.queryForObject(
                                "SELECT last_connected_at FROM device WHERE device_id = ?",
                                LocalDateTime.class,
                                s.deviceId())
                        .toInstant(ZoneOffset.UTC))
                .isAfterOrEqualTo(before);
        assertThat(jdbc.queryForObject(
                        "SELECT last_account_id FROM device WHERE device_id = ?", Long.class, s.deviceId()))
                .isEqualTo(s.accountId());
    }

    @Test
    void FR_013_endedSession_returns401() throws Exception {
        Session s = openSession("WEB");
        jdbc.update(
                "UPDATE login_session SET ended_at = ?, end_reason = 'LOGOUT' WHERE token_hash = ?",
                utc(time.now()),
                SessionTokens.hash(s.token()));

        mvc.perform(get(PROBE).cookie(new Cookie("session", s.token())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("SESSION_EXPIRED"));
    }

    @Test
    void BR_M15_21_webIdleBeyondCfgM15_10_endsSessionTimedOut() throws Exception {
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

    @Test
    void BR_M15_21_eachRequestRestartsIdleTimer() throws Exception {
        Session s = openSession("WEB");
        clock.advanceBy(Duration.ofMinutes(20));
        mvc.perform(get(PROBE).cookie(new Cookie("session", s.token()))).andExpect(status().isNotFound());
        clock.advanceBy(Duration.ofMinutes(20));

        mvc.perform(get(PROBE).cookie(new Cookie("session", s.token()))).andExpect(status().isNotFound());
    }

    @Test
    void BR_M15_21_limitIsPerChannel_staffAppKeepsSessionAfter31Minutes() throws Exception {
        Session s = openSession("STAFF_APP");
        clock.advanceBy(Duration.ofMinutes(31));

        mvc.perform(get(PROBE).header("Authorization", "Bearer " + s.token())).andExpect(status().isNotFound());
    }

    @Test
    void A_02_webWriteWithoutRequestedFromHeader_returns403() throws Exception {
        Session s = openSession("WEB");

        mvc.perform(post(PROBE).cookie(new Cookie("session", s.token())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        mvc.perform(post(PROBE).cookie(new Cookie("session", s.token())).header("X-Requested-From", "web"))
                .andExpect(status().isNotFound());
    }

    @Test
    void A_02_bearerWriteNeedsNoRequestedFromHeader() throws Exception {
        Session s = openSession("FAMILY_APP");

        mvc.perform(post(PROBE).header("Authorization", "Bearer " + s.token())).andExpect(status().isNotFound());
    }

    @Test
    void A_15_threePublicPaths_needNoSession() throws Exception {
        String stale = SessionTokens.generate();

        mvc.perform(post("/api/v1/auth/login")).andExpect(status().is(not(anyOf(is(401), is(403)))));
        mvc.perform(post("/api/v1/auth/qr-login").cookie(new Cookie("session", stale)))
                .andExpect(status().is(not(anyOf(is(401), is(403)))));
        mvc.perform(post("/api/v1/sos/alerts").header("Authorization", "Bearer " + stale))
                .andExpect(status().is(not(anyOf(is(401), is(403)))));
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isUnauthorized());
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

    private Instant lastActiveAt(Session s) {
        return jdbc.queryForObject(
                        "SELECT last_active_at FROM login_session WHERE token_hash = ?",
                        LocalDateTime.class,
                        SessionTokens.hash(s.token()))
                .toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record Session(String token, long accountId, String deviceId) {}
}
