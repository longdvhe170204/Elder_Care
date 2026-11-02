package com.eldercare.backend.incident.internal.alert.persistence;

import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Lưu trữ bảng cảnh báo {@code alert} (nhóm 2 STATEFUL).
 */
@Repository
public class AlertStore {

    private static final RowMapper<AlertRecord> MAPPER = AlertStore::mapRow;

    private final JdbcTemplate jdbc;

    public AlertStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(AlertRecord r) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                con -> {
                    var ps = con.prepareStatement("""
                            INSERT INTO alert (
                                resident_id, floor_area_id, type, type_lock, level, level_at_received,
                                occurrence_count, first_at, latest_at, status, escalation_level,
                                acknowledgement_deadline, max_escalation_at, max_reminder_count,
                                assignee_id, handover_acknowledged, receiver_id, received_at,
                                resolution, close_reason, closed_by_id, closed_by_system, closed_at,
                                incident_id, critical_flag_id, no_on_duty_doctor, escalation_stopped,
                                created_by_id, description, requester_id, arrived_at, arrived_by_id,
                                skip_arrival_time, version
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """, new String[] {"id"});
                    ps.setObject(1, r.residentId());
                    ps.setObject(2, r.floorAreaId());
                    ps.setString(3, r.type());
                    ps.setString(4, r.typeLock());
                    ps.setString(5, r.level() != null ? r.level().name() : null);
                    ps.setString(
                            6, r.levelAtReceived() != null ? r.levelAtReceived().name() : null);
                    ps.setInt(7, r.occurrenceCount());
                    ps.setTimestamp(8, Timestamp.from(r.firstAt()));
                    ps.setTimestamp(9, Timestamp.from(r.latestAt()));
                    ps.setString(10, r.status() != null ? r.status().name() : "NEW");
                    ps.setInt(11, r.escalationLevel());
                    ps.setTimestamp(
                            12,
                            r.acknowledgementDeadline() != null ? Timestamp.from(r.acknowledgementDeadline()) : null);
                    ps.setTimestamp(13, r.maxEscalationAt() != null ? Timestamp.from(r.maxEscalationAt()) : null);
                    ps.setInt(14, r.maxReminderCount());
                    ps.setObject(15, r.assigneeId());
                    ps.setBoolean(16, r.handoverAcknowledged());
                    ps.setObject(17, r.receiverId());
                    ps.setTimestamp(18, r.receivedAt() != null ? Timestamp.from(r.receivedAt()) : null);
                    ps.setString(19, r.resolution());
                    ps.setString(20, r.closeReason());
                    ps.setObject(21, r.closedById());
                    ps.setBoolean(22, r.closedBySystem());
                    ps.setTimestamp(23, r.closedAt() != null ? Timestamp.from(r.closedAt()) : null);
                    ps.setObject(24, r.incidentId());
                    ps.setObject(25, r.criticalFlagId());
                    ps.setBoolean(26, r.noOnDutyDoctor());
                    ps.setBoolean(27, r.escalationStopped());
                    ps.setObject(28, r.createdById());
                    ps.setString(29, r.description());
                    ps.setObject(30, r.requesterId());
                    ps.setTimestamp(31, r.arrivedAt() != null ? Timestamp.from(r.arrivedAt()) : null);
                    ps.setObject(32, r.arrivedById());
                    ps.setBoolean(33, r.skipArrivalTime());
                    ps.setInt(34, r.version());
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<AlertRecord> findOpenAlert(long residentId, String typeLock) {
        try {
            var record = jdbc.queryForObject("""
                    SELECT * FROM alert
                    WHERE resident_id = ? AND type_lock = ? AND status IN ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS')
                    LIMIT 1
                    """, MAPPER, residentId, typeLock);
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<AlertRecord> findById(long id) {
        try {
            var record = jdbc.queryForObject("SELECT * FROM alert WHERE id = ?", MAPPER, id);
            return Optional.ofNullable(record);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<AlertRecord> findByResident(long residentId, Instant from, Instant to) {
        return jdbc.query("""
                SELECT * FROM alert
                WHERE resident_id = ? AND first_at >= ? AND first_at <= ?
                ORDER BY first_at DESC
                """, MAPPER, residentId, Timestamp.from(from), Timestamp.from(to));
    }

    public boolean hasOpenAlert(long residentId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM alert
                WHERE resident_id = ? AND status IN ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS')
                """, Integer.class, residentId);
        return count != null && count > 0;
    }

    public void updateMerged(
            long alertId,
            int newCount,
            Instant latestAt,
            IncidentLevel newLevel,
            Instant deadline,
            int expectedVersion) {
        jdbc.update(
                """
                UPDATE alert SET
                    occurrence_count = ?,
                    latest_at = ?,
                    level = ?,
                    acknowledgement_deadline = ?,
                    version = version + 1
                WHERE id = ? AND version = ?
                """,
                newCount,
                Timestamp.from(latestAt),
                newLevel.name(),
                deadline != null ? Timestamp.from(deadline) : null,
                alertId,
                expectedVersion);
    }

    private static AlertRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new AlertRecord(
                rs.getLong("id"),
                (Long) rs.getObject("resident_id"),
                (Long) rs.getObject("floor_area_id"),
                rs.getString("type"),
                rs.getString("type_lock"),
                rs.getString("level") != null ? IncidentLevel.valueOf(rs.getString("level")) : null,
                rs.getString("level_at_received") != null
                        ? IncidentLevel.valueOf(rs.getString("level_at_received"))
                        : null,
                rs.getInt("occurrence_count"),
                toInstant(rs.getTimestamp("first_at")),
                toInstant(rs.getTimestamp("latest_at")),
                rs.getString("status") != null ? AlertStatus.valueOf(rs.getString("status")) : AlertStatus.NEW,
                rs.getInt("escalation_level"),
                toInstant(rs.getTimestamp("acknowledgement_deadline")),
                toInstant(rs.getTimestamp("max_escalation_at")),
                rs.getInt("max_reminder_count"),
                (Long) rs.getObject("assignee_id"),
                rs.getBoolean("handover_acknowledged"),
                (Long) rs.getObject("receiver_id"),
                toInstant(rs.getTimestamp("received_at")),
                rs.getString("resolution"),
                rs.getString("close_reason"),
                (Long) rs.getObject("closed_by_id"),
                rs.getBoolean("closed_by_system"),
                toInstant(rs.getTimestamp("closed_at")),
                (Long) rs.getObject("incident_id"),
                (Long) rs.getObject("critical_flag_id"),
                rs.getBoolean("no_on_duty_doctor"),
                rs.getBoolean("escalation_stopped"),
                (Long) rs.getObject("created_by_id"),
                rs.getString("description"),
                (Long) rs.getObject("requester_id"),
                toInstant(rs.getTimestamp("arrived_at")),
                (Long) rs.getObject("arrived_by_id"),
                rs.getBoolean("skip_arrival_time"),
                rs.getInt("version"));
    }

    private static Instant toInstant(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }
}
