package com.eldercare.backend.common.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Audit cơ sở dữ liệu F-11, F-12 (2026-10-29): job dọn {@code idempotency_key} đã qua {@code expires_at} và sự kiện
 * {@code EVENT_PUBLICATION} đã giao xong quá 7 ngày; khóa còn hạn, sự kiện chưa giao xong hoặc mới giao xong được giữ
 * (data-model 000 mục 6, research R-09).
 */
class TechnicalDataCleanupIT extends MySqlIntegrationTestBase {

    @Autowired
    TechnicalDataCleanup cleanup;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    /** Giờ của ứng dụng (đồng hồ giả lập ở profile test), như job dùng. */
    private LocalDateTime now() {
        return LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC);
    }

    private String key(LocalDateTime expiresAt) {
        String key = UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO idempotency_key (key_value, username, request_hash, expires_at) VALUES (?, 'f11', ?, ?)",
                key,
                "0".repeat(64),
                expiresAt);
        return key;
    }

    private String event(LocalDateTime completedAt) {
        String id = UUID.randomUUID().toString();
        jdbc.update(
                "INSERT INTO EVENT_PUBLICATION (ID, LISTENER_ID, EVENT_TYPE, SERIALIZED_EVENT, PUBLICATION_DATE,"
                        + " COMPLETION_DATE, STATUS) VALUES (?, 'f12', 'F12', '{}', ?, ?, ?)",
                id,
                now().minusDays(30),
                completedAt,
                completedAt == null ? "PUBLISHED" : "COMPLETED");
        return id;
    }

    @Test
    void F_11_expiredIdempotencyKeys_areRemoved_validOnesKept() {
        String expired = key(now().minusDays(1));
        String valid = key(now().plusDays(1));

        cleanup.run();

        assertThat(count("SELECT COUNT(*) FROM idempotency_key WHERE key_value = ?", expired))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM idempotency_key WHERE key_value = ?", valid))
                .isEqualTo(1);
    }

    @Test
    void F_12_completedEventsOlderThan7Days_areRemoved_othersKept() {
        String old = event(now().minusDays(8));
        String recent = event(now().minusDays(1));
        String incomplete = event(null);
        try {
            cleanup.run();

            assertThat(count("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE ID = ?", old))
                    .isZero();
            assertThat(count("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE ID = ?", recent))
                    .isEqualTo(1);
            assertThat(count("SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE ID = ?", incomplete))
                    .isEqualTo(1);
        } finally {
            // Sự kiện mẫu chưa giao xong không có listener: xóa để lần khởi động sau không giao lại
            jdbc.update("DELETE FROM EVENT_PUBLICATION WHERE LISTENER_ID = 'f12'");
        }
    }

    private int count(String sql, String id) {
        return jdbc.queryForObject(sql, Integer.class, id);
    }
}
