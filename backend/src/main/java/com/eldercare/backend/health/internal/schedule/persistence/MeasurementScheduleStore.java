package com.eldercare.backend.health.internal.schedule.persistence;

import com.eldercare.backend.health.internal.schedule.domain.MeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.NewMeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.ScheduleFrequency;
import com.eldercare.backend.health.internal.schedule.domain.StoredScheduleContent;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lịch đo trong {@code monitoring_setting} (kind MEASUREMENT_SCHEDULE, V007_09): nội dung FR-003 ở JSON
 * {@code components}, không sửa được (FR-005, trigger); chỉ đổi trạng thái, {@code stopped_at}, lý do ngừng.
 */
@Repository
public class MeasurementScheduleStore {
    private static final String COLUMNS =
            "id, resident_id, effective_from, status, components, replaces_id, stopped_at, ended_reason, version";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public MeasurementScheduleStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public long insert(MeasurementSchedule schedule, long createdBy, Instant createdAt) {
        var content = schedule.content();
        var frequency = content.frequency();
        var stored = new StoredScheduleContent(
                List.copyOf(new TreeSet<>(content.indicatorIds())),
                frequency.type(),
                frequency.times().stream().map(t -> t.toString()).toList(),
                frequency.everyHours(),
                frequency.anchorAt(),
                frequency.weekdays(),
                content.endAt(),
                content.priority(),
                content.notes());
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps = connection.prepareStatement("""
                            INSERT INTO monitoring_setting (kind, scope, resident_id, indicator_id, effective_from,
                              status, components, replaces_id, created_by, created_at)
                            VALUES ('MEASUREMENT_SCHEDULE', 'PERSONAL', ?, ?, ?, ?, ?, ?, ?, ?)
                            """, Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, schedule.residentId());
                    ps.setLong(2, stored.indicatorIds().getFirst());
                    ps.setTimestamp(3, Timestamp.from(content.startAt()));
                    ps.setString(4, schedule.status());
                    ps.setString(5, json.writeValueAsString(stored));
                    ps.setObject(6, schedule.replaces());
                    ps.setLong(7, createdBy);
                    ps.setTimestamp(8, Timestamp.from(createdAt));
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<MeasurementSchedule> lock(long id) {
        return query("WHERE id = ? FOR UPDATE", id).stream().findFirst();
    }

    public Optional<MeasurementSchedule> find(long id) {
        return query("WHERE id = ?", id).stream().findFirst();
    }

    public List<MeasurementSchedule> byResident(long residentId) {
        return query("WHERE resident_id = ? ORDER BY effective_from DESC, id DESC", residentId);
    }

    public List<MeasurementSchedule> openByResident(long residentId) {
        return query("WHERE resident_id = ? AND status IN ('PENDING','EFFECTIVE') ORDER BY id", residentId);
    }

    public List<MeasurementSchedule> duePending(Instant at) {
        return query("WHERE status = 'PENDING' AND effective_from <= ? ORDER BY id", Timestamp.from(at));
    }

    /** Lịch Hiệu lực đã tới thời điểm kết thúc (kết thúc nằm trong JSON). */
    public List<MeasurementSchedule> dueExpiry(Instant at) {
        return query("WHERE status = 'EFFECTIVE' ORDER BY id").stream()
                .filter(s -> s.content().endAt() != null && !s.content().endAt().isAfter(at))
                .toList();
    }

    /** Đã ngừng tại {@code stoppedAt}; {@code stoppedBy} trống là Hệ thống. */
    public void stop(long id, Instant stoppedAt, String reason, Long stoppedBy, int expectedVersion) {
        int changed = jdbc.update("""
                UPDATE monitoring_setting SET status = 'STOPPED', stopped_at = ?, ended_reason = ?, ended_by = ?,
                  version = version + 1
                WHERE id = ? AND kind = 'MEASUREMENT_SCHEDULE' AND status IN ('PENDING','EFFECTIVE') AND version = ?
                """, Timestamp.from(stoppedAt), reason, stoppedBy, id, expectedVersion);
        if (changed != 1) throw new IllegalStateException("Lịch đo đã thay đổi đồng thời");
    }

    public void activate(long id, int expectedVersion) {
        change(id, "PENDING", "EFFECTIVE", expectedVersion);
    }

    public void expire(long id, int expectedVersion) {
        change(id, "EFFECTIVE", "EXPIRED", expectedVersion);
    }

    private void change(long id, String from, String to, int expectedVersion) {
        int changed = jdbc.update(
                "UPDATE monitoring_setting SET status = ?, version = version + 1"
                        + " WHERE id = ? AND kind = 'MEASUREMENT_SCHEDULE' AND status = ? AND version = ?",
                to,
                id,
                from,
                expectedVersion);
        if (changed != 1) throw new IllegalStateException("Lịch đo đã thay đổi đồng thời");
    }

    private List<MeasurementSchedule> query(String where, Object... args) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM monitoring_setting WHERE kind = 'MEASUREMENT_SCHEDULE' AND "
                        + where.replaceFirst("^WHERE ", ""),
                this::map,
                args);
    }

    private MeasurementSchedule map(ResultSet rs, int row) throws SQLException {
        var stored = json.readValue(rs.getString("components"), StoredScheduleContent.class);
        var stopped = rs.getTimestamp("stopped_at");
        var content = new NewMeasurementSchedule(
                Set.copyOf(stored.indicatorIds()),
                new ScheduleFrequency(
                        stored.frequencyType(),
                        stored.times() == null
                                ? List.of()
                                : stored.times().stream().map(LocalTime::parse).toList(),
                        stored.everyHours(),
                        stored.anchorAt(),
                        stored.weekdays()),
                rs.getTimestamp("effective_from").toInstant(),
                stored.endAt(),
                stored.priority(),
                stored.notes());
        return new MeasurementSchedule(
                rs.getLong("id"),
                rs.getLong("resident_id"),
                content,
                rs.getString("status"),
                (Long) rs.getObject("replaces_id"),
                stopped == null ? null : stopped.toInstant(),
                rs.getString("ended_reason"),
                rs.getInt("version"));
    }
}
