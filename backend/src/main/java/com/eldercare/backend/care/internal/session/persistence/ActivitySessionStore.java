package com.eldercare.backend.care.internal.session.persistence;

import com.eldercare.backend.care.internal.session.domain.ListedSession;
import com.eldercare.backend.care.internal.session.domain.Registration;
import com.eldercare.backend.care.internal.session.domain.Session;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** 014 T008: SQL trên {@code activity_session}, {@code activity_registration}; giờ lưu theo giờ Việt Nam (DBR-25). */
@Repository
public class ActivitySessionStore {
    private static final String COLUMNS =
            "id,activity_id,starts_at,ends_at,source,location_room_id,location_area_id,floor_area_id,"
                    + "responsible_staff_id,status,incomplete_attendance,cancel_reason,cancelled_by,version,trip_status";

    private final JdbcTemplate jdbc;

    public ActivitySessionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static LocalDateTime local(Instant at) {
        return LocalDateTime.ofInstant(at, BusinessTime.ZONE);
    }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        var value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.atZone(BusinessTime.ZONE).toInstant();
    }

    private static Session session(ResultSet rs, int i) throws SQLException {
        return new Session(
                rs.getLong("id"),
                rs.getLong("activity_id"),
                instant(rs, "starts_at"),
                instant(rs, "ends_at"),
                rs.getString("source"),
                rs.getObject("location_room_id", Long.class),
                rs.getObject("location_area_id", Long.class),
                rs.getObject("floor_area_id", Long.class),
                rs.getObject("responsible_staff_id", Long.class),
                rs.getString("status"),
                rs.getBoolean("incomplete_attendance"),
                rs.getString("cancel_reason"),
                rs.getString("cancelled_by"),
                rs.getInt("version"),
                rs.getString("trip_status"));
    }

    private static Registration registration(ResultSet rs, int i) throws SQLException {
        return new Registration(
                rs.getLong("id"),
                rs.getLong("session_id"),
                rs.getLong("resident_id"),
                rs.getString("source"),
                rs.getObject("registered_by_staff_id", Long.class),
                rs.getString("status"));
    }

    public long insert(Session s, long createdBy, Instant at) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var p = c.prepareStatement(
                            "INSERT INTO activity_session(activity_id,starts_at,ends_at,source,location_room_id,"
                                    + "location_area_id,floor_area_id,responsible_staff_id,status,created_by,created_at)"
                                    + " VALUES(?,?,?,?,?,?,?,?,'SCHEDULED',?,?)",
                            new String[] {"id"});
                    p.setLong(1, s.activityId());
                    p.setObject(2, local(s.startsAt()));
                    p.setObject(3, local(s.endsAt()));
                    p.setString(4, s.source());
                    p.setObject(5, s.locationRoomId());
                    p.setObject(6, s.locationAreaId());
                    p.setObject(7, s.floorAreaId());
                    p.setObject(8, s.responsibleStaffId());
                    p.setLong(9, createdBy);
                    p.setObject(10, local(at));
                    return p;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<Session> find(long id) {
        return jdbc
                .query("SELECT " + COLUMNS + " FROM activity_session WHERE id=?", ActivitySessionStore::session, id)
                .stream()
                .findFirst();
    }

    public Session lock(long id) {
        return jdbc
                .query(
                        "SELECT " + COLUMNS + " FROM activity_session WHERE id=? FOR UPDATE",
                        ActivitySessionStore::session,
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException());
    }

    /** FR-007: mọi thời điểm bắt đầu đã có (kể cả Đã hủy) của hoạt động trong khoảng. */
    public Set<Instant> starts(long activityId, Instant from, Instant to) {
        return new HashSet<>(jdbc.query(
                "SELECT starts_at FROM activity_session WHERE activity_id=? AND starts_at>=? AND starts_at<?",
                (rs, i) -> instant(rs, "starts_at"),
                activityId,
                local(from),
                local(to)));
    }

    public boolean exists(long activityId, Instant startsAt) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM activity_session WHERE activity_id=? AND starts_at=?)",
                Boolean.class,
                activityId,
                local(startsAt)));
    }

    /** Buổi Đã lên lịch của hoạt động bắt đầu từ {@code from}, khóa dòng. */
    public List<Session> scheduledFrom(long activityId, Instant from) {
        return jdbc.query(
                "SELECT " + COLUMNS
                        + " FROM activity_session WHERE activity_id=? AND status='SCHEDULED' AND starts_at>=?"
                        + " ORDER BY starts_at FOR UPDATE",
                ActivitySessionStore::session,
                activityId,
                local(from));
    }

    public List<Session> scheduledOn(LocalDate day) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM activity_session WHERE status='SCHEDULED' AND trip_status IS NULL"
                        + " AND starts_at>=? AND starts_at<?"
                        + " ORDER BY id FOR UPDATE",
                ActivitySessionStore::session,
                day.atStartOfDay(),
                day.plusDays(1).atStartOfDay());
    }

    public Optional<LocalDate> oldestScheduledBefore(LocalDate today) {
        var value = jdbc.queryForObject(
                "SELECT MIN(starts_at) FROM activity_session WHERE status='SCHEDULED' AND trip_status IS NULL AND starts_at<?",
                LocalDateTime.class,
                today.atStartOfDay());
        return Optional.ofNullable(value).map(LocalDateTime::toLocalDate);
    }

    public List<ListedSession> list(LocalDate from, LocalDate to, Long floorAreaId) {
        var sql = new StringBuilder("SELECT s.")
                .append(COLUMNS.replace(",", ",s."))
                .append(
                        ", (SELECT COUNT(*) FROM activity_registration r WHERE r.session_id=s.id AND r.status<>'CANCELLED') n"
                                + " FROM activity_session s WHERE s.starts_at>=? AND s.starts_at<?");
        var args = new ArrayList<Object>(
                List.of(from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
        if (floorAreaId != null) {
            sql.append(" AND s.floor_area_id=?");
            args.add(floorAreaId);
        }
        sql.append(" ORDER BY s.starts_at, s.id");
        return jdbc.query(sql.toString(), (rs, i) -> new ListedSession(session(rs, i), rs.getInt("n")), args.toArray());
    }

    public void cancel(long id, String by, String reason, Long staffId) {
        jdbc.update(
                "UPDATE activity_session SET status='CANCELLED', cancelled_by=?, cancel_reason=?, cancelled_by_staff_id=?,"
                        + " version=version+1 WHERE id=?",
                by,
                reason,
                staffId,
                id);
    }

    public void reschedule(long id, Instant startsAt, Instant endsAt, Long roomId, Long responsibleStaffId) {
        jdbc.update(
                "UPDATE activity_session SET starts_at=?, ends_at=?, location_room_id=COALESCE(?,location_room_id),"
                        + " responsible_staff_id=COALESCE(?,responsible_staff_id), version=version+1 WHERE id=?",
                local(startsAt),
                local(endsAt),
                roomId,
                responsibleStaffId,
                id);
    }

    public void changeLocationFloor(long id, long floorAreaId) {
        jdbc.update("UPDATE activity_session SET floor_area_id=?,location_area_id=NULL WHERE id=?", floorAreaId, id);
    }

    public void close(long id, String status, boolean incomplete) {
        jdbc.update(
                "UPDATE activity_session SET status=?, incomplete_attendance=?, version=version+1 WHERE id=?",
                status,
                incomplete,
                id);
    }

    // Đăng ký

    public long register(long sessionId, long residentId, String source, Long staffId, Instant at) {
        return register(sessionId, residentId, source, staffId, at, null);
    }

    /** {@code acknowledgedWarnings}: JSON danh sách cảnh báo đã hiển thị và được xác nhận (FR-018), có thể rỗng. */
    public long register(
            long sessionId, long residentId, String source, Long staffId, Instant at, String acknowledgedWarnings) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    var p = c.prepareStatement(
                            "INSERT INTO activity_registration(session_id,resident_id,source,registered_by_staff_id,"
                                    + "registered_at,status,acknowledged_warnings,participant_status) VALUES(?,?,?,?,?,'REGISTERED',?,"
                                    + "IF((SELECT trip_status FROM activity_session WHERE id=?) IS NULL,NULL,'REGISTERED'))",
                            new String[] {"id"});
                    p.setLong(1, sessionId);
                    p.setLong(2, residentId);
                    p.setString(3, source);
                    p.setObject(4, staffId);
                    p.setObject(5, local(at));
                    p.setString(6, acknowledgedWarnings);
                    p.setLong(7, sessionId);
                    return p;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Registration lockRegistration(long id) {
        return jdbc
                .query(
                        "SELECT * FROM activity_registration WHERE id=? FOR UPDATE",
                        ActivitySessionStore::registration,
                        id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException());
    }

    /** Buổi Đã lên lịch bắt đầu sau {@code from} của mọi hoạt động, khóa dòng (FR-021). */
    public List<Session> scheduledAfter(Instant from) {
        return jdbc.query(
                "SELECT " + COLUMNS
                        + " FROM activity_session WHERE status='SCHEDULED' AND (trip_status IS NULL OR trip_status='SCHEDULED') AND starts_at>? ORDER BY id FOR UPDATE",
                ActivitySessionStore::session,
                local(from));
    }

    /** Buổi trong viện chưa bắt đầu hoặc người chưa rời viện của chuyến còn mở; theo người cao tuổi khi có. */
    public List<Registration> registeredAfter(Instant from, Long residentId) {
        return jdbc.query(
                "SELECT r.* FROM activity_registration r JOIN activity_session s ON s.id=r.session_id"
                        + " WHERE r.status='REGISTERED' AND s.status='SCHEDULED' AND ((s.trip_status IS NULL AND s.starts_at>?) OR (s.trip_status IN ('SCHEDULED','IN_PROGRESS') AND r.participant_status IN ('REGISTERED','ELIGIBLE')))"
                        + (residentId == null ? "" : " AND r.resident_id=" + residentId)
                        + " ORDER BY r.session_id, r.id FOR UPDATE",
                ActivitySessionStore::registration,
                local(from));
    }

    /** FR-030: buổi Đã lên lịch kết thúc không sau {@code cutoff}, chưa nhắc; khóa dòng. */
    public List<Session> dueForAttendanceReminder(Instant cutoff) {
        return jdbc.query(
                "SELECT " + COLUMNS
                        + " FROM activity_session WHERE status='SCHEDULED' AND trip_status IS NULL AND attendance_reminded_at IS NULL"
                        + " AND ends_at<=? ORDER BY id FOR UPDATE",
                ActivitySessionStore::session,
                local(cutoff));
    }

    public void markAttendanceReminded(long id, Instant at) {
        jdbc.update("UPDATE activity_session SET attendance_reminded_at=? WHERE id=?", local(at), id);
    }

    public Optional<Registration> findRegistration(long id) {
        return jdbc
                .query("SELECT * FROM activity_registration WHERE id=?", ActivitySessionStore::registration, id)
                .stream()
                .findFirst();
    }

    /** 005 FR-017: đăng ký chưa hủy của người cao tuổi ở buổi Đã lên lịch giao với khoảng [from, to). */
    public List<Registration> registrationsInWindow(long residentId, Instant from, Instant to) {
        return jdbc.query(
                "SELECT r.* FROM activity_registration r JOIN activity_session s ON s.id=r.session_id"
                        + " WHERE r.resident_id=? AND r.status<>'CANCELLED' AND s.status='SCHEDULED'"
                        + " AND s.starts_at<? AND s.ends_at>? ORDER BY s.starts_at, r.id",
                ActivitySessionStore::registration,
                residentId,
                local(to),
                local(from));
    }

    /** 014 FR-068 (cổng người thân): đăng ký chưa hủy của người cao tuổi ở buổi chưa hủy bắt đầu trong [from, to). */
    public List<Registration> registrationsStartingIn(long residentId, Instant from, Instant to) {
        return jdbc.query(
                "SELECT r.* FROM activity_registration r JOIN activity_session s ON s.id=r.session_id"
                        + " WHERE r.resident_id=? AND r.status<>'CANCELLED' AND s.status<>'CANCELLED'"
                        + " AND s.starts_at>=? AND s.starts_at<? ORDER BY s.starts_at, r.id",
                ActivitySessionStore::registration,
                residentId,
                local(from),
                local(to));
    }

    /** 014 FR-068: số buổi người cao tuổi đã tham gia (đăng ký Đã tham gia) bắt đầu trong [from, to). */
    public int attendedCount(long residentId, Instant from, Instant to) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM activity_registration r JOIN activity_session s ON s.id=r.session_id"
                        + " WHERE r.resident_id=? AND r.status='ATTENDED' AND s.starts_at>=? AND s.starts_at<?",
                Integer.class,
                residentId,
                local(from),
                local(to));
        return count == null ? 0 : count;
    }

    /** Đăng ký chưa hủy của người cao tuổi ở buổi, khóa dòng. */
    public Optional<Registration> activeRegistration(long sessionId, long residentId) {
        return activeRegistration(sessionId, residentId, true);
    }

    public Optional<Registration> findActiveRegistration(long sessionId, long residentId) {
        return activeRegistration(sessionId, residentId, false);
    }

    private Optional<Registration> activeRegistration(long sessionId, long residentId, boolean lock) {
        return jdbc
                .query(
                        "SELECT * FROM activity_registration WHERE session_id=? AND resident_id=? AND status<>'CANCELLED'"
                                + (lock ? " FOR UPDATE" : ""),
                        ActivitySessionStore::registration,
                        sessionId,
                        residentId)
                .stream()
                .findFirst();
    }

    public List<Long> dueLateReturnConsents(Instant now) {
        return jdbc.queryForList(
                "SELECT c.id FROM simple_record c JOIN activity_session s ON s.id=c.entity_id"
                        + " JOIN activity_registration r ON r.session_id=s.id AND r.resident_id=c.resident_id"
                        + " WHERE c.kind='M04.LATE_RETURN_CONSENT' AND c.status='ACTIVE' AND r.status<>'CANCELLED'"
                        + " AND s.status<>'CANCELLED' AND COALESCE(s.trip_planned_departure_at,s.starts_at)<=?"
                        + " ORDER BY s.id,c.id",
                Long.class,
                local(now));
    }

    /** FR-031, FR-032: nguồn tính phí đang hiệu lực của đăng ký; rỗng khi chưa có hoặc đã hủy. */
    public Optional<String> billedSource(long registrationId) {
        return Optional.ofNullable(jdbc.queryForObject(
                "SELECT billed_source_id FROM activity_registration WHERE id=? FOR UPDATE",
                String.class,
                registrationId));
    }

    public void billedSource(long registrationId, String sourceId) {
        jdbc.update("UPDATE activity_registration SET billed_source_id=? WHERE id=?", sourceId, registrationId);
    }

    /** Đăng ký còn hiệu lực (chưa hủy) của buổi. */
    public List<Registration> activeRegistrations(long sessionId) {
        return jdbc.query(
                "SELECT * FROM activity_registration WHERE session_id=? AND status<>'CANCELLED' ORDER BY id FOR UPDATE",
                ActivitySessionStore::registration,
                sessionId);
    }

    public boolean registered(long sessionId, long residentId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM activity_registration WHERE session_id=? AND resident_id=? AND status<>'CANCELLED')",
                Boolean.class,
                sessionId,
                residentId));
    }

    /** FR-016: đăng ký còn hiệu lực ở buổi khác (chưa hủy) chồng thời gian. */
    public boolean overlaps(long residentId, long sessionId, Instant from, Instant to) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM activity_registration r JOIN activity_session s ON s.id=r.session_id"
                        + " WHERE r.resident_id=? AND r.status<>'CANCELLED' AND s.status<>'CANCELLED' AND s.id<>?"
                        + " AND s.starts_at<? AND s.ends_at>?)",
                Boolean.class,
                residentId,
                sessionId,
                local(to),
                local(from)));
    }

    public void cancelRegistration(long id, String autoCancel, String reason) {
        jdbc.update(
                "UPDATE activity_registration SET status='CANCELLED', auto_cancel=?, cancel_reason=?,"
                        + " participant_status=IF(participant_status IS NULL,NULL,'NOT_GOING'),participant_reason=?,version=version+1"
                        + " WHERE id=? AND status='REGISTERED'",
                autoCancel,
                reason,
                reason,
                id);
    }

    public void markAttended(long id) {
        jdbc.update(
                "UPDATE activity_registration SET status='ATTENDED', version=version+1 WHERE id=? AND status='REGISTERED'",
                id);
    }
}
