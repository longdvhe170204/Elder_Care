package com.eldercare.backend.resident.internal.assessment.persistence;

import com.eldercare.backend.resident.internal.assessment.domain.CareLevelChangeDecision;
import com.eldercare.backend.resident.internal.assessment.domain.CareLevelChangeDecision.Decision;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Quyết định khi gia đình không đồng ý đổi mức chăm sóc ({@code care_level_change_decision}, nhóm 3; 001 FR-036b, Q-424).
 * {@code UNIQUE(assessment_id)} giữ tối đa một quyết định mỗi lần đánh giá; trigger chỉ cho đặt {@code review_notified_at}.
 */
@Repository
public class CareLevelChangeDecisionStore {

    private final JdbcTemplate jdbc;

    CareLevelChangeDecisionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(
            long residentId,
            long assessmentId,
            Decision decision,
            Long riskAgreementId,
            LocalDate reviewDate,
            String note,
            long recordedBy,
            Instant at) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps = connection.prepareStatement(
                            "INSERT INTO care_level_change_decision (resident_id, assessment_id, decision,"
                                    + " risk_agreement_id, review_date, note, recorded_by, recorded_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            java.sql.Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, residentId);
                    ps.setLong(2, assessmentId);
                    ps.setString(3, decision.name());
                    ps.setObject(4, riskAgreementId);
                    ps.setObject(5, reviewDate);
                    ps.setString(6, note);
                    ps.setLong(7, recordedBy);
                    ps.setObject(8, LocalDateTime.ofInstant(at, ZoneOffset.UTC));
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    public boolean existsFor(long assessmentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM care_level_change_decision WHERE assessment_id = ?)",
                Boolean.class,
                assessmentId));
    }

    public List<CareLevelChangeDecision> ofResident(long residentId) {
        return jdbc.query(
                "SELECT * FROM care_level_change_decision WHERE resident_id = ? ORDER BY recorded_at DESC, id DESC",
                (rs, i) -> map(rs),
                residentId);
    }

    /** Quyết định giữ mức đã tới ngày xem lại mà chưa nhắc. */
    public List<CareLevelChangeDecision> reviewDue(LocalDate today) {
        return jdbc.query(
                "SELECT * FROM care_level_change_decision WHERE decision = 'KEEP_CURRENT_LEVEL' AND review_date <= ?"
                        + " AND review_notified_at IS NULL ORDER BY id",
                (rs, i) -> map(rs),
                today);
    }

    /** Đặt dấu đã nhắc; {@code false} khi đã có (lần chạy khác đã nhắc). */
    public boolean markReviewNotified(long id, Instant at) {
        return jdbc.update(
                        "UPDATE care_level_change_decision SET review_notified_at = ? WHERE id = ?"
                                + " AND review_notified_at IS NULL",
                        LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                        id)
                == 1;
    }

    private static CareLevelChangeDecision map(ResultSet rs) throws SQLException {
        LocalDateTime notified = rs.getObject("review_notified_at", LocalDateTime.class);
        return new CareLevelChangeDecision(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                rs.getLong("assessment_id"),
                Decision.valueOf(rs.getString("decision")),
                rs.getObject("risk_agreement_id", Long.class),
                rs.getObject("review_date", LocalDate.class),
                rs.getString("note"),
                rs.getLong("recorded_by"),
                rs.getObject("recorded_at", LocalDateTime.class).toInstant(ZoneOffset.UTC),
                notified == null ? null : notified.toInstant(ZoneOffset.UTC));
    }
}
