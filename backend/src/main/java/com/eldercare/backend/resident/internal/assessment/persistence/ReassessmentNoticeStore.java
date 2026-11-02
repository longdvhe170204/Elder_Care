package com.eldercare.backend.resident.internal.assessment.persistence;

import com.eldercare.backend.resident.internal.assessment.domain.PeriodicReassessmentDue;
import com.eldercare.backend.resident.internal.assessment.domain.ReassessmentBasis;
import com.eldercare.backend.resident.internal.assessment.domain.ReassessmentNotice;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Bảng kỹ thuật {@code reassessment_notice} (research H-11): lần báo "chưa đáp ứng" là lần sau lần đánh giá Đã xác nhận
 * gần nhất; bộ {@code (resident_id, basis, source_type, source_id)} chống báo trùng.
 */
@Repository
public class ReassessmentNoticeStore {

    private static final String LATEST_CONFIRMED =
            "COALESCE((SELECT MAX(a.confirmed_at) FROM assessment a WHERE a.resident_id = n.resident_id"
                    + " AND a.status = 'CONFIRMED'), '1000-01-01')";

    private final JdbcTemplate jdbc;

    ReassessmentNoticeStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Mọi lần báo chưa đáp ứng của hồ sơ tính hạn từ {@code from}, xóa dấu đã cảnh báo quá hạn. */
    public void restartDeadlines(long residentId, Instant from) {
        jdbc.update(
                "UPDATE reassessment_notice n SET n.deadline_from = ?, n.overdue_alerted_at = NULL"
                        + " WHERE n.resident_id = ? AND n.notified_at > " + LATEST_CONFIRMED,
                Timestamp.from(from),
                residentId);
    }

    /** Hồ sơ ở bốn trạng thái của FR-039 có lần Đã xác nhận gần nhất không muộn hơn {@code before}. */
    public List<PeriodicReassessmentDue> periodicDue(Instant before) {
        return jdbc.query(
                "SELECT r.id, a.id FROM resident r JOIN assessment a ON a.id = (SELECT l.id FROM assessment l"
                        + " WHERE l.resident_id = r.id AND l.status = 'CONFIRMED' ORDER BY l.confirmed_at DESC,"
                        + " l.id DESC LIMIT 1) WHERE r.status IN ('IN_RESIDENCE', 'TEMPORARILY_ABSENT',"
                        + " 'OFF_SITE_ACTIVITY', 'HOSPITALIZED') AND a.confirmed_at <= ?",
                (rs, i) -> new PeriodicReassessmentDue(rs.getLong(1), rs.getLong(2)),
                Timestamp.from(before));
    }

    public void markOverdueAlerted(long noticeId, Instant at) {
        jdbc.update("UPDATE reassessment_notice SET overdue_alerted_at = ? WHERE id = ?", Timestamp.from(at), noticeId);
    }

    /** Mã người cao tuổi đang có lần báo chưa đáp ứng. */
    public List<Long> residentsWithUnmetNotices() {
        return jdbc.query(
                "SELECT DISTINCT n.resident_id FROM reassessment_notice n WHERE n.notified_at > " + LATEST_CONFIRMED
                        + " ORDER BY n.resident_id",
                (rs, i) -> rs.getLong(1));
    }

    /** Thêm lần báo; rỗng khi đã báo cùng căn cứ và nguồn. */
    public Optional<Long> insert(
            long residentId,
            ReassessmentBasis basis,
            String sourceType,
            long sourceId,
            Instant notifiedAt,
            Instant deadlineFrom) {
        int inserted = jdbc.update(
                "INSERT IGNORE INTO reassessment_notice (resident_id, basis, source_type, source_id, notified_at,"
                        + " deadline_from) VALUES (?, ?, ?, ?, ?, ?)",
                residentId,
                basis.name(),
                sourceType,
                sourceId,
                Timestamp.from(notifiedAt),
                deadlineFrom == null ? null : Timestamp.from(deadlineFrom));
        if (inserted == 0) {
            return Optional.empty();
        }
        return Optional.of(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class));
    }

    /** Các lần báo chưa đáp ứng của hồ sơ, sớm nhất trước. */
    public List<ReassessmentNotice> unmet(long residentId) {
        return jdbc.query(
                "SELECT n.id, n.basis, n.source_type, n.source_id, n.notified_at, n.deadline_from,"
                        + " n.overdue_alerted_at FROM reassessment_notice n WHERE n.resident_id = ? AND n.notified_at > "
                        + LATEST_CONFIRMED + " ORDER BY n.notified_at, n.id",
                (rs, i) -> new ReassessmentNotice(
                        rs.getLong(1),
                        ReassessmentBasis.valueOf(rs.getString(2)),
                        rs.getString(3),
                        rs.getLong(4),
                        rs.getTimestamp(5).toInstant(),
                        instant(rs.getTimestamp(6)),
                        instant(rs.getTimestamp(7))),
                residentId);
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
