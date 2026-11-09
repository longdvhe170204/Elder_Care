package com.eldercare.backend.incident.internal.incident.persistence;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.domain.IncidentEscalationState;
import com.eldercare.backend.incident.internal.incident.domain.IncidentState;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import com.eldercare.backend.incident.internal.incident.domain.NewIncident;
import com.eldercare.backend.incident.internal.incident.domain.SuspectedIncident;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
        return suspected(id, " FOR UPDATE");
    }

    /** Đọc không khóa cho tra cứu truy vết và xem danh sách tiếp xúc (FR-060, FR-061). */
    public Optional<SuspectedIncident> findSuspected(long id) {
        return suspected(id, "");
    }

    private Optional<SuspectedIncident> suspected(long id, String lock) {
        return jdbc.query(
                """
                SELECT id,resident_id,discovered_at,status,floor_area_id FROM incident
                WHERE id=? AND type_code='SUSPECTED_INFECTION'
                """ + lock,
                rs -> rs.next()
                        ? Optional.of(new SuspectedIncident(
                                rs.getLong("id"),
                                rs.getLong("resident_id"),
                                rs.getTimestamp("discovered_at").toInstant(),
                                rs.getString("status"),
                                rs.getObject("floor_area_id", Long.class)))
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
                              created_at, confirmed_at, was_emergency, acknowledgement_deadline, no_on_duty_doctor,
                              source_alert_id)
                            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
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
                    ps.setObject(23, incident.sourceAlertId());
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

    /** Sự cố Mới, Đang xử lý, Đang theo dõi của người cao tuổi (CHK061, research S-17). */
    public boolean hasOpen(long residentId) {
        return !jdbc.queryForList(
                        "SELECT 1 FROM incident WHERE resident_id = ? AND status IN ('NEW','IN_PROGRESS','MONITORING')"
                                + " LIMIT 1",
                        Integer.class,
                        residentId)
                .isEmpty();
    }

    /** Dòng đọc cho module khác (module-interfaces 007 mục 1.7): loại hiện hành, mức, trạng thái lưu trong bảng. */
    public Optional<com.eldercare.backend.incident.api.dto.IncidentView> sourceView(long id) {
        return jdbc.query(SOURCE_COLUMNS + " WHERE id = ?", (rs, i) -> sourceRow(rs), id).stream()
                .findFirst();
    }

    /** Sự cố của người cao tuổi lập trong [from, to); {@code excludeCancelled} bỏ Đã hủy. */
    public List<com.eldercare.backend.incident.api.dto.IncidentView> sourceByResident(
            long residentId, Instant from, Instant to, boolean excludeCancelled) {
        return jdbc.query(
                SOURCE_COLUMNS + " WHERE resident_id = ? AND created_at >= ? AND created_at < ?"
                        + (excludeCancelled ? " AND status <> 'CANCELLED'" : "")
                        + " ORDER BY created_at, id",
                (rs, i) -> sourceRow(rs),
                residentId,
                Timestamp.from(from),
                Timestamp.from(to));
    }

    private static final String SOURCE_COLUMNS =
            "SELECT id, resident_id, type_code, level, status, created_at, floor_area_id FROM incident";

    private static com.eldercare.backend.incident.api.dto.IncidentView sourceRow(java.sql.ResultSet rs)
            throws java.sql.SQLException {
        long resident = rs.getLong("resident_id");
        Long residentId = rs.wasNull() ? null : resident;
        long floor = rs.getLong("floor_area_id");
        Long floorAreaId = rs.wasNull() ? null : floor;
        return new com.eldercare.backend.incident.api.dto.IncidentView(
                rs.getLong("id"),
                residentId,
                rs.getString("type_code"),
                rs.getString("level"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                floorAreaId);
    }

    /** Còn sự cố mở mức Trung bình trở lên (014 FR-018). */
    public boolean hasOpenAtLeastMedium(long residentId) {
        return !jdbc.queryForList(
                        "SELECT 1 FROM incident WHERE resident_id = ? AND status IN ('NEW','IN_PROGRESS','MONITORING')"
                                + " AND level IN ('MEDIUM','CRITICAL') LIMIT 1",
                        Integer.class,
                        residentId)
                .isEmpty();
    }

    /** Người cao tuổi từng có sự cố (mục việc sau qua đời, research S-17). */
    public boolean hasAny(long residentId) {
        return !jdbc.queryForList("SELECT 1 FROM incident WHERE resident_id = ? LIMIT 1", Integer.class, residentId)
                .isEmpty();
    }

    /** Người cao tuổi chuyển trạng thái cuối: dừng leo thang và nhắc mọi sự cố đang mở (FR-038). */
    public int stopEscalation(long residentId) {
        return jdbc.update("""
                UPDATE incident SET escalation_stopped = TRUE, version = version + 1
                WHERE resident_id = ? AND status IN ('NEW', 'IN_PROGRESS', 'MONITORING')
                  AND escalation_stopped = FALSE
                """, residentId);
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

    /** Sự cố Mới quá hạn tiếp nhận, chưa dừng leo thang (FR-044, research S-11, S-21). */
    public List<Long> dueForEscalation(Instant now, int limit) {
        return jdbc.queryForList("""
                SELECT id FROM incident
                WHERE status = 'NEW' AND escalation_stopped = FALSE
                  AND acknowledgement_deadline IS NOT NULL AND acknowledgement_deadline <= ?
                ORDER BY acknowledgement_deadline, id LIMIT ?
                """, Long.class, Timestamp.from(now), limit);
    }

    public Optional<IncidentEscalationState> lockForEscalation(long id) {
        return jdbc.query(
                """
                SELECT id, resident_id, floor_area_id, status, level, escalation_level, acknowledgement_deadline,
                  max_escalation_at, max_reminder_count, escalation_stopped, version
                FROM incident WHERE id = ? FOR UPDATE
                """,
                rs -> rs.next()
                        ? Optional.of(new IncidentEscalationState(
                                rs.getLong("id"),
                                (Long) rs.getObject("resident_id"),
                                (Long) rs.getObject("floor_area_id"),
                                rs.getString("status"),
                                IncidentLevel.valueOf(rs.getString("level")),
                                rs.getInt("escalation_level"),
                                instant(rs.getTimestamp("acknowledgement_deadline")),
                                instant(rs.getTimestamp("max_escalation_at")),
                                rs.getInt("max_reminder_count"),
                                rs.getBoolean("escalation_stopped"),
                                rs.getInt("version")))
                        : Optional.empty(),
                id);
    }

    /** Lên một cấp, trạng thái giữ Mới (FR-044). */
    public void escalate(long id, int level, Instant deadline, int expectedVersion) {
        int changed = jdbc.update("""
                UPDATE incident SET escalation_level = ?, acknowledgement_deadline = ?, version = version + 1
                WHERE id = ? AND status = 'NEW' AND version = ?
                """, level, timestamp(deadline), id, expectedVersion);
        if (changed != 1) throw new IllegalStateException("Sự cố đã thay đổi đồng thời");
    }

    /** Cấp 2 vẫn quá hạn: dấu "đã leo thang tối đa", lần nhắc đầu tới hạn ở {@code nextDeadline} (FR-034). */
    public void reachMaximum(long id, Instant maximumAt, Instant nextDeadline, int expectedVersion) {
        int changed = jdbc.update("""
                UPDATE incident SET max_escalation_at = ?, acknowledgement_deadline = ?, version = version + 1
                WHERE id = ? AND status = 'NEW' AND version = ?
                """, Timestamp.from(maximumAt), timestamp(nextDeadline), id, expectedVersion);
        if (changed != 1) throw new IllegalStateException("Sự cố đã thay đổi đồng thời");
    }

    public void reminded(long id, int count, Instant nextDeadline, int expectedVersion) {
        int changed = jdbc.update("""
                UPDATE incident SET max_reminder_count = ?, acknowledgement_deadline = ?, version = version + 1
                WHERE id = ? AND status = 'NEW' AND version = ?
                """, count, timestamp(nextDeadline), id, expectedVersion);
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

    public Optional<Long> assignee(long id) {
        return Optional.ofNullable(
                jdbc.queryForObject("SELECT assignee_id FROM incident WHERE id = ?", Long.class, id));
    }

    /** Giao từ Mới: như Tiếp nhận xử lý, người tiếp nhận là người giao (research S-11, FR-045a). */
    public void assignFromNew(long id, long assigneeId, long assignerId, Instant at, int expectedVersion) {
        int changed = jdbc.update("""
                UPDATE incident SET status = 'IN_PROGRESS', assignee_id = ?, received_at = ?, received_by = ?,
                  escalation_stopped = TRUE, version = version + 1
                WHERE id = ? AND status = 'NEW' AND version = ?
                """, assigneeId, Timestamp.from(at), assignerId, id, expectedVersion);
        if (changed != 1) throw new IllegalStateException("Sự cố đã thay đổi đồng thời");
    }

    /** Giao lại khi Đang xử lý, Đang theo dõi: chỉ đổi người xử lý. */
    public void reassign(long id, long assigneeId, int expectedVersion) {
        int changed = jdbc.update("""
                UPDATE incident SET assignee_id = ?, version = version + 1
                WHERE id = ? AND status IN ('IN_PROGRESS', 'MONITORING') AND version = ?
                """, assigneeId, id, expectedVersion);
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

    /** FR-047b: lúc kích hoạt khẩn cấp không có Bác sĩ trực; dòng đã khóa bởi lệnh đang chạy. */
    public void markNoOnDutyDoctor(long id) {
        jdbc.update("UPDATE incident SET no_on_duty_doctor = TRUE, version = version + 1 WHERE id = ?", id);
    }

    public Optional<IncidentView> view(long id) {
        return jdbc.query("SELECT * FROM incident WHERE id = ?", (rs, i) -> viewRow(rs), id).stream()
                .findFirst();
    }

    /** Danh sách theo phạm vi: toàn viện, hoặc tầng/người cao tuổi trong tập; mới nhất trước, tối đa 500 dòng. */
    public List<IncidentView> search(
            Collection<String> statuses,
            Long residentId,
            Long floorAreaId,
            boolean facilityWide,
            Set<Long> scopeFloors,
            Set<Long> scopeResidents) {
        var sql = new StringBuilder("SELECT * FROM incident WHERE 1 = 1");
        var args = new ArrayList<Object>();
        if (!facilityWide) {
            sql.append(" AND (FALSE");
            if (!scopeFloors.isEmpty()) {
                sql.append(" OR floor_area_id IN (")
                        .append(String.join(",", Collections.nCopies(scopeFloors.size(), "?")))
                        .append(")");
                args.addAll(scopeFloors);
            }
            if (!scopeResidents.isEmpty()) {
                sql.append(" OR resident_id IN (")
                        .append(String.join(",", Collections.nCopies(scopeResidents.size(), "?")))
                        .append(")");
                args.addAll(scopeResidents);
            }
            sql.append(")");
        }
        if (statuses != null && !statuses.isEmpty()) {
            sql.append(" AND status IN (")
                    .append(String.join(",", Collections.nCopies(statuses.size(), "?")))
                    .append(")");
            args.addAll(statuses);
        }
        if (residentId != null) {
            sql.append(" AND resident_id = ?");
            args.add(residentId);
        }
        if (floorAreaId != null) {
            sql.append(" AND floor_area_id = ?");
            args.add(floorAreaId);
        }
        sql.append(" ORDER BY created_at DESC, id DESC LIMIT 500");
        return jdbc.query(sql.toString(), (rs, i) -> viewRow(rs), args.toArray());
    }

    private static IncidentView viewRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new IncidentView(
                rs.getLong("id"),
                (Long) rs.getObject("resident_id"),
                rs.getLong("incident_type_id"),
                rs.getLong("current_type_id"),
                rs.getString("context"),
                instant(rs.getTimestamp("occurred_at")),
                instant(rs.getTimestamp("discovered_at")),
                (Long) rs.getObject("floor_area_id"),
                rs.getString("location"),
                rs.getString("ongoing_activity"),
                rs.getString("description"),
                rs.getString("initial_response"),
                IncidentLevel.valueOf(rs.getString("initial_level")),
                rs.getString("lower_level_reason"),
                (Long) rs.getObject("source_alert_id"),
                rs.getBoolean("out_of_scope"),
                rs.getLong("recorded_by"),
                rs.getString("status"),
                IncidentLevel.valueOf(rs.getString("level")),
                rs.getBoolean("was_emergency"),
                rs.getInt("escalation_level"),
                (Long) rs.getObject("assignee_id"),
                instant(rs.getTimestamp("received_at")),
                rs.getBoolean("no_on_duty_doctor"),
                rs.getString("resolution"));
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    /** Hủy ghi nhận bằng đính chính: Đã đóng/đang mở -> Đã hủy (BR-M05-21). */
    public void cancel(long id) {
        jdbc.update(
                "UPDATE incident SET status = 'CANCELLED', escalation_stopped = TRUE, version = version + 1 WHERE id = ?",
                id);
    }

    /** Đổi loại bằng đính chính; nội dung gốc giữ nguyên ({@code incident_type_id}). */
    public void changeType(long id, long typeId, String typeCode) {
        jdbc.update(
                "UPDATE incident SET current_type_id = ?, type_code = ?, version = version + 1 WHERE id = ?",
                typeId,
                typeCode,
                id);
    }
}
