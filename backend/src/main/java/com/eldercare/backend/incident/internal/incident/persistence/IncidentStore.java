package com.eldercare.backend.incident.internal.incident.persistence;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.domain.IncidentState;
import com.eldercare.backend.incident.internal.incident.domain.NewIncident;
import com.eldercare.backend.incident.internal.incident.domain.SuspectedIncident;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Nguồn sự cố đã ghi nhận, bao gồm dấu nghi nhiễm tính tại thời điểm truy vấn. */
@Repository
public class IncidentStore {
    private final JdbcTemplate jdbc;

    public IncidentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean suspectedAt(long residentId, Instant at) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM incident
                    WHERE resident_id = ? AND type_code = 'SUSPECTED_INFECTION'
                      AND created_at <= ? AND status <> 'CANCELLED'
                      AND (closed_at IS NULL OR ? < closed_at)
                )
                """, Boolean.class, residentId, Timestamp.from(at), Timestamp.from(at)));
    }

    public Set<Long> lockOpenSuspected(Set<Long> ids) {
        if (ids.isEmpty()) return Set.of();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        return new HashSet<>(jdbc.queryForList("""
                SELECT id FROM incident WHERE id IN (%s)
                  AND type_code = 'SUSPECTED_INFECTION'
                  AND status IN ('NEW','IN_PROGRESS','MONITORING')
                ORDER BY id FOR UPDATE
                """.formatted(placeholders), Long.class, ids.toArray()));
    }

    public Optional<SuspectedIncident> lockSuspected(long id) {
        return jdbc.query(
                """
                SELECT id,resident_id,discovered_at,status FROM incident
                WHERE id=? AND type_code='SUSPECTED_INFECTION' FOR UPDATE
                """,
                rs -> rs.next()
                        ? Optional.of(new SuspectedIncident(
                                rs.getLong("id"),
                                rs.getLong("resident_id"),
                                rs.getTimestamp("discovered_at").toInstant(),
                                rs.getString("status")))
                        : Optional.empty(),
                id);
    }

    public Optional<IncidentState> lock(long id) {
        return jdbc.query(
                """
                SELECT id,resident_id,floor_area_id,status,level,was_emergency,version
                FROM incident WHERE id=? FOR UPDATE
                """,
                rs -> rs.next()
                        ? Optional.of(new IncidentState(
                                rs.getLong("id"),
                                (Long) rs.getObject("resident_id"),
                                (Long) rs.getObject("floor_area_id"),
                                rs.getString("status"),
                                IncidentLevel.valueOf(rs.getString("level")),
                                rs.getBoolean("was_emergency"),
                                rs.getInt("version")))
                        : Optional.empty(),
                id);
    }

    public long insert(NewIncident incident) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps = connection.prepareStatement("""
                            INSERT INTO incident (resident_id, incident_type_id, current_type_id, type_code, context,
                              occurred_at, discovered_at, floor_area_id, location, ongoing_activity, description,
                              initial_response, initial_level, level, lower_level_reason, out_of_scope, recorded_by,
                              created_at, confirmed_at, was_emergency, acknowledgement_deadline, no_on_duty_doctor)
                            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                            """, Statement.RETURN_GENERATED_KEYS);
                    ps.setObject(1, incident.residentId());
                    ps.setLong(2, incident.typeId());
                    ps.setLong(3, incident.typeId());
                    ps.setString(4, incident.typeCode());
                    ps.setString(5, incident.context());
                    ps.setTimestamp(6, timestamp(incident.occurredAt()));
                    ps.setTimestamp(7, timestamp(incident.discoveredAt()));
                    ps.setObject(8, incident.floorAreaId());
                    ps.setString(9, incident.location());
                    ps.setString(10, incident.ongoingActivity());
                    ps.setString(11, incident.description());
                    ps.setString(12, incident.initialResponse());
                    ps.setString(13, incident.level().name());
                    ps.setString(14, incident.level().name());
                    ps.setString(15, incident.lowerLevelReason());
                    ps.setBoolean(16, incident.outOfScope());
                    ps.setLong(17, incident.recordedBy());
                    ps.setTimestamp(18, timestamp(incident.recordedAt()));
                    ps.setTimestamp(19, timestamp(incident.recordedAt()));
                    ps.setBoolean(20, incident.level() == IncidentLevel.CRITICAL);
                    ps.setTimestamp(21, timestamp(incident.acknowledgementDeadline()));
                    ps.setBoolean(22, incident.noOnDutyDoctor());
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Sự cố Khẩn cấp đang mở của người cao tuổi (FR-041); gọi sau khi khóa theo dõi người đó. */
    public Optional<Long> openEmergency(long residentId) {
        return jdbc
                .queryForList(
                        "SELECT id FROM incident WHERE resident_id = ? AND open_emergency = 1 ORDER BY id LIMIT 1",
                        Long.class,
                        residentId)
                .stream()
                .findFirst();
    }

    private static Timestamp timestamp(Instant at) {
        return at == null ? null : Timestamp.from(at);
    }

    /** Người ghi nhận sự cố; được ghi diễn biến dù ngoài phạm vi (bảng FR-045). */
    public long recordedBy(long id) {
        return jdbc.queryForObject("SELECT recorded_by FROM incident WHERE id = ?", Long.class, id);
    }

    public String currentTypeCode(long id) {
        return jdbc.queryForObject("SELECT type_code FROM incident WHERE id = ?", String.class, id);
    }

    /** Chuyển trạng thái theo bảng FR-045 khi trạng thái và phiên bản còn như lúc khóa. */
    public void changeStatus(long id, String from, String to, int expectedVersion) {
        int changed = jdbc.update(
                "UPDATE incident SET status = ?, version = version + 1 WHERE id = ? AND status = ? AND version = ?",
                to,
                id,
                from,
                expectedVersion);
        if (changed != 1) throw new IllegalStateException("Sự cố đã thay đổi đồng thời");
    }

    /** Đóng sự cố: kết quả bắt buộc, dừng leo thang và nhắc (bảng FR-045, BR-M05-09). */
    public void close(
            long id, String resolution, String infectionResult, long closedBy, Instant at, int expectedVersion) {
        int changed = jdbc.update("""
                UPDATE incident SET status = 'CLOSED', resolution = ?, infection_result = ?, closed_by = ?,
                  closed_at = ?, escalation_stopped = TRUE, version = version + 1
                WHERE id = ? AND status IN ('IN_PROGRESS', 'MONITORING') AND version = ?
                """, resolution, infectionResult, closedBy, Timestamp.from(at), id, expectedVersion);
        if (changed != 1) throw new IllegalStateException("Sự cố đã thay đổi đồng thời");
    }

    public void receive(long id, long receiverId, Instant at, int expectedVersion) {
        int changed = jdbc.update("""
                UPDATE incident SET status='IN_PROGRESS', received_at=?, received_by=?, assignee_id=?,
                  escalation_stopped=TRUE, version=version+1
                WHERE id=? AND status='NEW' AND version=?
                """, Timestamp.from(at), receiverId, receiverId, id, expectedVersion);
        if (changed != 1) throw new IllegalStateException("Sự cố đã thay đổi đồng thời");
    }

    public void changeLevel(long id, IncidentLevel level, boolean wasEmergency, int expectedVersion) {
        int changed = jdbc.update("""
                UPDATE incident SET level=?, was_emergency=?, version=version+1
                WHERE id=? AND version=?
                """, level.name(), wasEmergency, id, expectedVersion);
        if (changed != 1) throw new IllegalStateException("Sự cố đã thay đổi đồng thời");
    }
}
