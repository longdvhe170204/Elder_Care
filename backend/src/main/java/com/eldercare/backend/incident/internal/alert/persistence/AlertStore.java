package com.eldercare.backend.incident.internal.alert.persistence;

import com.eldercare.backend.incident.internal.alert.domain.AlertReceiveState;
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
 * Lưu trữ bảng cảnh báo {@code alert} (nhóm 2 BUSINESS).
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

    /** Cảnh báo nút hỗ trợ đang mở của người bấm (cột sinh {@code open_assist_lock}, FR-047i). */
    public Optional<AlertRecord> findOpenAssist(long requesterId) {
        return jdbc
                .query(
                        "SELECT * FROM alert WHERE type = 'STAFF_ASSIST_REQUEST' AND requester_id = ?"
                                + " AND status IN ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS') LIMIT 1",
                        MAPPER,
                        requesterId)
                .stream()
                .findFirst();
    }

    /** Gắn người cao tuổi cho cảnh báo chưa gắn, một lần (trigger chỉ cho NULL → giá trị với nút hỗ trợ). */
    public void attachResident(long id, long residentId) {
        requireOne(jdbc.update(
                "UPDATE alert SET resident_id = ?, version = version + 1 WHERE id = ? AND resident_id IS NULL",
                residentId,
                id));
    }

    /** Đặt tầng cho cảnh báo chưa có tầng (người bấm chọn sau, FR-047i); người phụ trách nếu chưa ai tiếp nhận. */
    public void attachFloor(long id, long floorAreaId, Long assigneeId) {
        requireOne(jdbc.update(
                "UPDATE alert SET floor_area_id = ?, assignee_id = COALESCE(assignee_id, ?), version = version + 1"
                        + " WHERE id = ? AND floor_area_id IS NULL",
                floorAreaId,
                assigneeId,
                id));
    }

    /** Mốc "Đã tới nơi", một lần, không đổi trạng thái (FR-047j). */
    public void markArrived(long id, long byId, Instant at) {
        requireOne(jdbc.update(
                "UPDATE alert SET arrived_at = ?, arrived_by_id = ?, version = version + 1"
                        + " WHERE id = ? AND arrived_at IS NULL",
                Timestamp.from(at),
                byId,
                id));
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

    /** Người cao tuổi từng có cảnh báo (mục việc sau qua đời, research S-17). */
    public boolean hasAnyAlert(long residentId) {
        return !jdbc.queryForList("SELECT 1 FROM alert WHERE resident_id = ? LIMIT 1", Integer.class, residentId)
                .isEmpty();
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

    public Optional<AlertReceiveState> lockForReceive(long id) {
        return jdbc.query(
                """
                SELECT id, resident_id, floor_area_id, status, escalation_level, level, receiver_id, received_at, version
                FROM alert WHERE id = ? FOR UPDATE
                """,
                rs -> rs.next()
                        ? Optional.of(new AlertReceiveState(
                                rs.getLong("id"),
                                (Long) rs.getObject("resident_id"),
                                (Long) rs.getObject("floor_area_id"),
                                AlertStatus.valueOf(rs.getString("status")),
                                rs.getInt("escalation_level"),
                                rs.getString("level") == null ? null : IncidentLevel.valueOf(rs.getString("level")),
                                (Long) rs.getObject("receiver_id"),
                                toInstant(rs.getTimestamp("received_at")),
                                rs.getInt("version")))
                        : Optional.empty(),
                id);
    }

    /** Tiếp nhận: người tiếp nhận thành người phụ trách, dừng leo thang, giữ mức lúc tiếp nhận (FR-029, FR-081). */
    public void receive(long id, long receiverId, Instant at, IncidentLevel level, int expectedVersion) {
        int changed = jdbc.update(
                """
                UPDATE alert SET status = 'RECEIVED', receiver_id = ?, received_at = ?, assignee_id = ?,
                  level_at_received = ?, escalation_stopped = TRUE, version = version + 1
                WHERE id = ? AND status IN ('NEW', 'ESCALATED') AND version = ?
                """,
                receiverId,
                Timestamp.from(at),
                receiverId,
                level == null ? null : level.name(),
                id,
                expectedVersion);
        if (changed != 1) throw new IllegalStateException("Cảnh báo đã thay đổi đồng thời");
    }

    /** Cảnh báo Mới, Leo thang quá hạn tiếp nhận, chưa dừng leo thang (FR-033, research S-08, S-21). */
    public List<Long> dueForEscalation(Instant now, int limit) {
        return jdbc.queryForList("""
                SELECT id FROM alert
                WHERE status IN ('NEW', 'ESCALATED') AND escalation_stopped = FALSE
                  AND acknowledgement_deadline IS NOT NULL AND acknowledgement_deadline <= ?
                ORDER BY acknowledgement_deadline, id LIMIT ?
                """, Long.class, Timestamp.from(now), limit);
    }

    public Optional<AlertRecord> lock(long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject("SELECT * FROM alert WHERE id = ? FOR UPDATE", MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** Lên một cấp: trạng thái Leo thang, hạn tiếp nhận mới (FR-033, FR-084 a). */
    public void escalate(long id, int level, Instant deadline, int expectedVersion) {
        requireOne(jdbc.update("""
                UPDATE alert SET status = 'ESCALATED', escalation_level = ?, acknowledgement_deadline = ?,
                  version = version + 1
                WHERE id = ? AND status IN ('NEW', 'ESCALATED') AND version = ?
                """, level, deadline == null ? null : Timestamp.from(deadline), id, expectedVersion));
    }

    /** Cấp 2 vẫn quá hạn: mang dấu "đã leo thang tối đa", lần nhắc đầu tiên tới hạn sau một khoảng (FR-034). */
    public void reachMaximum(long id, Instant maximumAt, Instant nextDeadline, int expectedVersion) {
        requireOne(jdbc.update(
                """
                UPDATE alert SET status = 'ESCALATED', max_escalation_at = ?, acknowledgement_deadline = ?,
                  version = version + 1
                WHERE id = ? AND status IN ('NEW', 'ESCALATED') AND version = ?
                """,
                Timestamp.from(maximumAt),
                nextDeadline == null ? null : Timestamp.from(nextDeadline),
                id,
                expectedVersion));
    }

    /** Đã nhắc lần thứ {@code count}; lần kế tới hạn ở {@code nextDeadline} (FR-034, FR-084 d). */
    public void reminded(long id, int count, Instant nextDeadline, int expectedVersion) {
        requireOne(jdbc.update(
                """
                UPDATE alert SET max_reminder_count = ?, acknowledgement_deadline = ?, version = version + 1
                WHERE id = ? AND status = 'ESCALATED' AND version = ?
                """, count, nextDeadline == null ? null : Timestamp.from(nextDeadline), id, expectedVersion));
    }

    /** Đã tiếp nhận → Đang xử lý; người ra lệnh thành người phụ trách (bảng FR-029, FR-031). */
    public void startHandling(long id, long assigneeId, int expectedVersion) {
        requireOne(jdbc.update("""
                UPDATE alert SET status = 'IN_PROGRESS', assignee_id = ?, version = version + 1
                WHERE id = ? AND status = 'RECEIVED' AND version = ?
                """, assigneeId, id, expectedVersion));
    }

    /** Nâng, hạ mức; {@code deadline} rỗng khi không tính lại hạn (đã tiếp nhận, Khẩn cấp). */
    public void changeLevel(long id, IncidentLevel level, Instant deadline, int expectedVersion) {
        requireOne(jdbc.update(
                """
                UPDATE alert SET level = ?,
                  acknowledgement_deadline = IF(status IN ('NEW', 'ESCALATED'), ?, acknowledgement_deadline),
                  version = version + 1
                WHERE id = ? AND status IN ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS') AND version = ?
                """, level.name(), deadline == null ? null : Timestamp.from(deadline), id, expectedVersion));
    }

    /**
     * Đang xử lý → Đã đóng do người (bảng FR-029). Cảnh báo từ nút SOS, nút hỗ trợ đóng mà chưa ghi đã tới nơi mang dấu
     * "không ghi mốc tới nơi" (FR-047j).
     */
    public void close(long id, String resolution, String reason, long closedById, Instant at, int expectedVersion) {
        requireOne(jdbc.update("""
                UPDATE alert SET status = 'CLOSED', resolution = ?, close_reason = ?, closed_by_id = ?, closed_at = ?,
                  skip_arrival_time = (type IN ('RESIDENT_HELP_REQUEST', 'STAFF_ASSIST_REQUEST') AND arrived_at IS NULL),
                  escalation_stopped = TRUE, version = version + 1
                WHERE id = ? AND status = 'IN_PROGRESS' AND version = ?
                """, resolution, reason, closedById, Timestamp.from(at), id, expectedVersion));
    }

    /** Đang xử lý → Chuyển sự cố, gắn sự cố liên kết, dừng leo thang (bảng FR-029). */
    public void convert(long id, long incidentId, int expectedVersion) {
        requireOne(jdbc.update("""
                UPDATE alert SET status = 'CONVERTED_TO_INCIDENT', incident_id = ?, escalation_stopped = TRUE,
                  version = version + 1
                WHERE id = ? AND status = 'IN_PROGRESS' AND version = ?
                """, incidentId, id, expectedVersion));
    }

    /** Giao người phụ trách: đổi người, giữ trạng thái, cấp, hạn và dấu tạm nhận (bảng FR-029). */
    public void reassign(long id, long assigneeId, int expectedVersion) {
        requireOne(jdbc.update("""
                UPDATE alert SET assignee_id = ?, version = version + 1
                WHERE id = ? AND status IN ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS') AND version = ?
                """, assigneeId, id, expectedVersion));
    }

    /** Cảnh báo vừa tạo lúc không có Bác sĩ trực (FR-047b); cùng lệnh tạo (cảnh báo SOS, FR-047e). */
    public void markNoOnDutyDoctor(long id) {
        requireOne(jdbc.update("UPDATE alert SET no_on_duty_doctor = TRUE, version = version + 1 WHERE id = ?", id));
    }

    /** Gắn dấu nguy kịch vào cảnh báo vừa tạo cho dấu đó, cùng lệnh ghi dấu (FR-050b, DBR-34). */
    public void linkCriticalFlag(long id, long criticalFlagId) {
        requireOne(jdbc.update("""
                UPDATE alert SET critical_flag_id = ?, version = version + 1
                WHERE id = ? AND critical_flag_id IS NULL
                """, criticalFlagId, id));
    }

    /** Cảnh báo đang mở của các người cao tuổi (đọc cho 005, 008). */
    public List<AlertRecord> openByResidents(java.util.Collection<Long> residentIds) {
        if (residentIds.isEmpty()) return List.of();
        String marks = String.join(",", java.util.Collections.nCopies(residentIds.size(), "?"));
        return jdbc.query(
                "SELECT * FROM alert WHERE status IN ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS')"
                        + " AND resident_id IN (" + marks + ") ORDER BY first_at, id",
                MAPPER,
                residentIds.toArray());
    }

    /** Cảnh báo có dòng nguồn trỏ tới bản ghi của module khác (kind {@code M05.ALERT_SOURCE}): mã nguồn → mã cảnh báo. */
    public List<long[]> bySource(String sourceType, java.util.Collection<Long> sourceIds) {
        if (sourceIds.isEmpty()) return List.of();
        String marks = String.join(",", java.util.Collections.nCopies(sourceIds.size(), "?"));
        var args = new java.util.ArrayList<Object>();
        args.add(sourceType);
        args.addAll(sourceIds);
        return jdbc.query(
                "SELECT DISTINCT JSON_VALUE(s.content, '$.sourceId' RETURNING SIGNED) AS source_id, s.entity_id"
                        + " FROM record_event s WHERE s.kind = 'M05.ALERT_SOURCE' AND s.entity_type = 'M05.ALERT'"
                        + " AND JSON_VALUE(s.content, '$.sourceType') = ?"
                        + " AND JSON_VALUE(s.content, '$.sourceId' RETURNING SIGNED) IN (" + marks + ")"
                        + " ORDER BY source_id, s.entity_id",
                (rs, n) -> new long[] {rs.getLong(1), rs.getLong(2)},
                args.toArray());
    }

    /** Người cao tuổi chuyển trạng thái cuối: dừng leo thang và nhắc mọi cảnh báo đang mở (FR-038). */
    public int stopEscalation(long residentId) {
        return jdbc.update("""
                UPDATE alert SET escalation_stopped = TRUE, version = version + 1
                WHERE resident_id = ? AND status IN ('NEW', 'ESCALATED', 'RECEIVED', 'IN_PROGRESS')
                  AND escalation_stopped = FALSE
                """, residentId);
    }

    /**
     * Danh sách theo bộ lọc của {@code GET /alerts}. {@code scopeFloors}, {@code scopeResidents} rỗng cả hai là toàn
     * viện; ngược lại chỉ lấy cảnh báo thuộc một trong hai tập (FR-035 của 002).
     */
    public List<AlertRecord> search(
            java.util.Collection<AlertStatus> statuses,
            Long floorAreaId,
            Long residentId,
            Boolean maxEscalation,
            boolean facilityWide,
            java.util.Set<Long> scopeFloors,
            java.util.Set<Long> scopeResidents) {
        var sql = new StringBuilder("SELECT * FROM alert WHERE 1 = 1");
        var args = new java.util.ArrayList<Object>();
        if (!facilityWide) {
            sql.append(" AND (FALSE");
            if (!scopeFloors.isEmpty()) {
                sql.append(" OR floor_area_id IN (")
                        .append(String.join(",", java.util.Collections.nCopies(scopeFloors.size(), "?")))
                        .append(")");
                args.addAll(scopeFloors);
            }
            if (!scopeResidents.isEmpty()) {
                sql.append(" OR resident_id IN (")
                        .append(String.join(",", java.util.Collections.nCopies(scopeResidents.size(), "?")))
                        .append(")");
                args.addAll(scopeResidents);
            }
            sql.append(")");
        }
        if (statuses != null && !statuses.isEmpty()) {
            sql.append(" AND status IN (")
                    .append(String.join(",", java.util.Collections.nCopies(statuses.size(), "?")))
                    .append(")");
            statuses.forEach(status -> args.add(status.name()));
        }
        if (floorAreaId != null) {
            sql.append(" AND floor_area_id = ?");
            args.add(floorAreaId);
        }
        if (residentId != null) {
            sql.append(" AND resident_id = ?");
            args.add(residentId);
        }
        if (maxEscalation != null) {
            sql.append(maxEscalation ? " AND max_escalation_at IS NOT NULL" : " AND max_escalation_at IS NULL");
        }
        sql.append(" ORDER BY latest_at DESC, id DESC LIMIT 500");
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    private static void requireOne(int changed) {
        if (changed != 1) throw new IllegalStateException("Cảnh báo đã thay đổi đồng thời");
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
