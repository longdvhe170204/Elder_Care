package com.eldercare.backend.resident.internal.persistence;

import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Đọc chung theo bảng bản ghi con của hồ sơ người cao tuổi, dùng cho gắn phạm vi và lưu giữ (001 T011, T079). Tên bảng
 * là hằng do lớp cấu hình của module truyền vào, không lấy từ dữ liệu người dùng. {@code condition} là điều kiện thêm
 * (chuỗi rỗng hoặc dạng {@code " AND kind = 'END_OF_LIFE'"}) cho bảng gộp nhiều loại bản ghi.
 */
@Repository
public class ResidentRecordStore {

    /** Hồ sơ ở trạng thái cuối và lúc vào trạng thái hiện tại (dòng lịch sử mới nhất đến đúng trạng thái đó). */
    public static final String FINAL_STATUS_AT = "SELECT r.id AS resident_id, (SELECT MAX(h.occurred_at) FROM"
            + " resident_status_history h WHERE h.resident_id = r.id AND h.to_status = r.status) AS final_at"
            + " FROM resident r WHERE r.status IN ('RESIDENCY_ENDED', 'DECEASED', 'ADMISSION_CANCELLED')";

    private final JdbcTemplate jdbc;

    ResidentRecordStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Hồ sơ chứa bản ghi; rỗng khi mã không tồn tại. */
    public Optional<Long> residentIdOf(String table, String condition, long id) {
        return jdbc
                .queryForList("SELECT resident_id FROM " + table + " WHERE id = ?" + condition, Long.class, id)
                .stream()
                .findFirst();
    }

    /** Mã các bản ghi của một hồ sơ. */
    public List<Long> idsOfResident(String table, String condition, long residentId) {
        return jdbc.queryForList(
                "SELECT id FROM " + table + " WHERE resident_id = ?" + condition, Long.class, residentId);
    }

    /** Bản ghi của hồ sơ đã vào trạng thái cuối trước {@code cutoff}, theo lúc vào trạng thái cuối rồi mã. */
    public List<Candidate> expiredCandidates(String table, String condition, Instant cutoff, int batch) {
        return jdbc.query(
                "SELECT c.id, f.final_at FROM " + table + " c JOIN (" + FINAL_STATUS_AT
                        + ") f ON f.resident_id = c.resident_id WHERE f.final_at < ?" + condition
                        + " ORDER BY f.final_at,"
                        + " c.id LIMIT ?",
                (rs, i) -> new Candidate(
                        rs.getLong(1), rs.getObject(2, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC),
                batch);
    }
}
