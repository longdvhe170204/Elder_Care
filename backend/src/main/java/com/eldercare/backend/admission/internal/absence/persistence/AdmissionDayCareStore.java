package com.eldercare.backend.admission.internal.absence.persistence;

import com.eldercare.backend.admission.internal.absence.domain.ClosureDay;
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
 * Báo vắng và ngày nghỉ bán trú ở bảng gộp {@code absence} (dòng {@code DAY_CARE_NOTICE}, {@code CLOSURE_DAY}; data-model mục 0, 8;
 * FR-060, FR-060a). Thời điểm lưu theo UTC. Báo vắng là nhóm 3, khóa ngay khi thêm.
 */
@Repository
public class AdmissionDayCareStore {

    private static final String SELECT_CLOSURE =
            "SELECT id, `date`, reason, late_declaration, status, created_by FROM absence"
                    + " WHERE kind = 'CLOSURE_DAY'";

    private final JdbcTemplate jdbc;

    AdmissionDayCareStore(JdbcTemplate jdbc) {
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
                            "INSERT INTO absence (kind, resident_id, `date`, session_id, family_notified_at,"
                                    + " within_notice_period, status, created_by, created_at, locked_at)"
                                    + " VALUES ('DAY_CARE_NOTICE', ?, ?, ?, ?, ?, 'RECORDED', ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    s.setLong(1, residentId);
                    s.setDate(2, Date.valueOf(date));
                    s.setObject(3, sessionId);
                    s.setObject(4, utc(familyNotifiedAt));
                    s.setBoolean(5, withinNoticePeriod);
                    s.setLong(6, recordedBy);
                    s.setObject(7, utc(recordedAt));
                    s.setObject(8, utc(recordedAt));
                    return s;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Có báo vắng đủ hạn cho buổi hoặc cả ngày (Vắng có báo không tính vào số đăng ký; 004 research T-10). */
    public boolean noticedAbsence(long residentId, LocalDate date, long sessionId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM absence WHERE kind = 'DAY_CARE_NOTICE' AND resident_id = ? AND `date` = ?"
                        + " AND within_notice_period AND (session_id IS NULL OR session_id = ?)",
                Integer.class,
                residentId,
                Date.valueOf(date),
                sessionId);
        return count != null && count > 0;
    }

    public long insertClosure(LocalDate date, String reason, boolean late, long declaredBy, Instant declaredAt) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var s = connection.prepareStatement(
                            "INSERT INTO absence (kind, `date`, reason, late_declaration, status, created_by, created_at)"
                                    + " VALUES ('CLOSURE_DAY', ?, ?, ?, 'ACTIVE', ?, ?)",
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
        return jdbc.query(SELECT_CLOSURE + " AND id = ?", this::mapClosure, id).stream()
                .findFirst();
    }

    public boolean activeClosureExists(LocalDate date) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM absence WHERE active_date = ?", Integer.class, Date.valueOf(date));
        return count != null && count > 0;
    }

    public void cancelClosure(long id, String reason, long cancelledBy, Instant at) {
        jdbc.update(
                "UPDATE absence SET status = 'CANCELLED', cancel_reason = ?, cancelled_by = ?, cancelled_at = ?,"
                        + " locked_at = ? WHERE id = ?",
                reason,
                cancelledBy,
                utc(at),
                utc(at),
                id);
    }

    public List<ClosureDay> closures(LocalDate from, LocalDate to) {
        return jdbc.query(
                SELECT_CLOSURE + " AND (? IS NULL OR `date` >= ?) AND (? IS NULL OR `date` <= ?) ORDER BY `date`, id",
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
                rs.getLong("created_by"));
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
