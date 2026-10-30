package com.eldercare.backend.administration.account.internal.offline.persistence;

import com.eldercare.backend.administration.account.internal.offline.domain.PreviousSyncResult;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Chống lặp theo từng bản ghi của lô đồng bộ ở {@code idempotency_key} (002 T056; FR-041a, research A-11): cùng người
 * ghi, cùng khóa thì trả lại kết quả lần đầu.
 */
@Component
public class SyncStore {

    private final JdbcTemplate jdbc;

    SyncStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Giữ khóa cho lần chạy này (khóa hết hạn trước {@code now} được xóa trước); rỗng khi giữ được, có giá trị là khóa đã
     * có từ trước.
     */
    public Optional<PreviousSyncResult> claim(String key, String owner, String hash, Instant now, Instant expiresAt) {
        jdbc.update(
                "DELETE FROM idempotency_key WHERE key_value = ? AND username = ? AND expires_at <= ?",
                key,
                owner,
                utc(now));
        try {
            jdbc.update(
                    "INSERT INTO idempotency_key (key_value, username, request_hash, expires_at) VALUES (?, ?, ?, ?)",
                    key,
                    owner,
                    hash,
                    utc(expiresAt));
            return Optional.empty();
        } catch (DuplicateKeyException e) {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT request_hash, result_json FROM idempotency_key WHERE key_value = ? AND username = ?",
                    key,
                    owner);
            Object result = row.get("result_json");
            return Optional.of(new PreviousSyncResult(
                    (String) row.get("request_hash"),
                    Optional.ofNullable(result).map(Object::toString)));
        }
    }

    public void saveResult(String key, String owner, String resultJson) {
        jdbc.update(
                "UPDATE idempotency_key SET result_json = ? WHERE key_value = ? AND username = ?",
                resultJson,
                key,
                owner);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
