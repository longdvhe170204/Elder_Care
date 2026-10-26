package com.cambae.backend.m02.internal.absence.persistence;

import com.cambae.backend.m02.internal.absence.domain.ClosureDay;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Truy cập {@code day_care_absence_notice} và {@code day_care_closure_day} (data-model mục 8; FR-060, FR-060a). Thời điểm
 * lưu theo UTC.
 */
@Repository
public class DayCareStore {

    private final JdbcTemplate jdbc;

    DayCareStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insertNotice(
            long residentId,
            LocalDate date,
            Long sessionId,
            Instant familyNotifiedAt,
            boolean withinNoticePeriod,
            long recordedBy,
            Instant recordedAt) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO day_care_absence_notice (resident_id, `date`, session_id, family_notified_at,"
                                    + " within_notice_period, recorded_by, recorded_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, residentId);
                    s.setDate(2, Date.valueOf(date));
                    s.setObject(3, sessionId);
                    s.setObject(4, utc(familyNotifiedAt));
                    s.setBoolean(5, withinNoticePeriod);
                    s.setLong(6, recordedBy);
                    s.setObject(7, utc(recordedAt));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    public long insertClosure(LocalDate date, String reason, boolean late, long declaredBy, Instant declaredAt) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO day_care_closure_day (`date`, reason, late_declaration, declared_by,"
                                    + " declared_at) VALUES (?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setDate(1, Date.valueOf(date));
                    s.setString(2, reason);
                    s.setBoolean(3, late);
                    s.setLong(4, declaredBy);
                    s.setObject(5, utc(declaredAt));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<ClosureDay> findClosure(long id) {
        return jdbc
                .query(
                        "SELECT id, `date`, reason, late_declaration, status, declared_by FROM day_care_closure_day"
                                + " WHERE id = ?",
                        this::mapClosure,
                        id)
                .stream()
                .findFirst();
    }

    public boolean activeClosureExists(LocalDate date) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM day_care_closure_day WHERE active_date = ?", Integer.class, Date.valueOf(date));
        return count != null && count > 0;
    }

    public void cancelClosure(long id, String reason, long cancelledBy, Instant at) {
        jdbc.update(
                "UPDATE day_care_closure_day SET status = 'CANCELLED', cancel_reason = ?, cancelled_by = ?,"
                        + " cancelled_at = ? WHERE id = ?",
                reason,
                cancelledBy,
                utc(at),
                id);
    }

    public List<ClosureDay> closures(LocalDate from, LocalDate to) {
        return jdbc.query(
                "SELECT id, `date`, reason, late_declaration, status, declared_by FROM day_care_closure_day WHERE"
                        + " (? IS NULL OR `date` >= ?) AND (? IS NULL OR `date` <= ?) ORDER BY `date`, id",
                this::mapClosure,
                from == null ? null : Date.valueOf(from),
                from == null ? null : Date.valueOf(from),
                to == null ? null : Date.valueOf(to),
                to == null ? null : Date.valueOf(to));
    }

    private ClosureDay mapClosure(ResultSet rs, int row) throws SQLException {
        return new ClosureDay(
                rs.getLong("id"),
                rs.getObject("date", LocalDate.class),
                rs.getString("reason"),
                rs.getBoolean("late_declaration"),
                "ACTIVE".equals(rs.getString("status")),
                rs.getLong("declared_by"));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
