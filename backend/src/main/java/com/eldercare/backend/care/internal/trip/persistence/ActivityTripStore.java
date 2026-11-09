package com.eldercare.backend.care.internal.trip.persistence;

import com.eldercare.backend.care.internal.trip.domain.*;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ActivityTripStore {
    private final JdbcTemplate jdbc;

    public ActivityTripStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static LocalDateTime local(Instant at) {
        return LocalDateTime.ofInstant(at, BusinessTime.ZONE);
    }

    private static Instant instant(ResultSet rs, String col) throws SQLException {
        return rs.getObject(col, LocalDateTime.class).atZone(BusinessTime.ZONE).toInstant();
    }

    private static Trip row(ResultSet rs, int i) throws SQLException {
        return new Trip(
                rs.getLong("id"),
                rs.getLong("activity_id"),
                rs.getObject("floor_area_id", Long.class),
                new TripPlan(
                        rs.getString("trip_destination"),
                        instant(rs, "trip_planned_departure_at"),
                        instant(rs, "trip_planned_return_at"),
                        rs.getString("trip_transport"),
                        rs.getBoolean("trip_facility_vehicle")),
                rs.getString("trip_status"),
                rs.getInt("version"));
    }

    public Optional<Trip> find(long id) {
        return jdbc
                .query(
                        "SELECT * FROM activity_session WHERE id=? AND trip_status IS NOT NULL",
                        ActivityTripStore::row,
                        id)
                .stream()
                .findFirst();
    }

    public Trip lock(long id) {
        return jdbc
                .query(
                        "SELECT * FROM activity_session WHERE id=? AND trip_status IS NOT NULL FOR UPDATE",
                        ActivityTripStore::row,
                        id)
                .stream()
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public void initialize(long id, TripPlan p) {
        jdbc.update(
                "UPDATE activity_session SET trip_destination=?,trip_planned_departure_at=?,trip_planned_return_at=?,"
                        + "trip_transport=?,trip_facility_vehicle=?,trip_status='SCHEDULED',version=version+1 WHERE id=?",
                p.destination(),
                local(p.plannedDepartureAt()),
                local(p.plannedReturnAt()),
                p.transport(),
                p.facilityVehicle(),
                id);
    }

    /** Mỗi người một dòng: đăng ký mới nhất, để đăng ký lại sau Không đi/hủy thay dòng cũ. */
    public List<TripParticipant> participants(long id) {
        return jdbc.query(
                "SELECT id,resident_id,participant_status,participant_reason FROM activity_registration r"
                        + " WHERE session_id=? AND participant_status IS NOT NULL AND id=(SELECT MAX(l.id)"
                        + " FROM activity_registration l WHERE l.session_id=r.session_id AND l.resident_id=r.resident_id"
                        + " AND l.participant_status IS NOT NULL) ORDER BY id",
                (rs, i) -> new TripParticipant(
                        rs.getLong("id"),
                        rs.getLong("resident_id"),
                        rs.getString("participant_status"),
                        rs.getString("participant_reason")),
                id);
    }

    public void participant(long registrationId, String status, String reason) {
        jdbc.update(
                "UPDATE activity_registration SET participant_status=?,participant_reason=?,version=version+1 WHERE id=?",
                status,
                reason,
                registrationId);
    }

    /** Điều kiện UPDATE đọc trạng thái hiện hành, kể cả khi transaction đang giữ snapshot cũ. */
    public boolean markDepartedIfEligible(long registrationId) {
        return jdbc.update(
                        "UPDATE activity_registration SET participant_status='DEPARTED',participant_reason=NULL,version=version+1"
                                + " WHERE id=? AND status='REGISTERED' AND participant_status='ELIGIBLE'",
                        registrationId)
                == 1;
    }

    public void notGoing(long registrationId, String reason) {
        jdbc.update(
                "UPDATE activity_registration SET participant_status='NOT_GOING',participant_reason=?,status='CANCELLED',cancel_reason=?,version=version+1 WHERE id=?",
                reason,
                reason,
                registrationId);
    }

    public void start(long id) {
        jdbc.update(
                "UPDATE activity_session SET trip_status='IN_PROGRESS',version=version+1 WHERE id=? AND trip_status='SCHEDULED'",
                id);
    }

    public void dutiesChanged(long id) {
        jdbc.update("UPDATE activity_session SET version=version+1 WHERE id=? AND trip_status IS NOT NULL", id);
    }

    public void cancel(long id) {
        jdbc.update("UPDATE activity_session SET trip_status='CANCELLED',version=version+1 WHERE id=?", id);
    }

    public void remoteLeader(long id) {
        jdbc.update("UPDATE activity_session SET trip_remote_leader=TRUE,version=version+1 WHERE id=?", id);
    }

    /** FR-044, FR-050: chỉ người đang ở ngoài (Đã rời viện hoặc Thiếu khi về) được điểm danh về. */
    public boolean markReturnedIfOut(long registrationId) {
        return jdbc.update(
                        "UPDATE activity_registration SET participant_status='RETURNED',participant_reason=NULL,version=version+1"
                                + " WHERE id=? AND participant_status IN ('DEPARTED','MISSING_ON_RETURN')",
                        registrationId)
                == 1;
    }

    public boolean markMissingIfDeparted(long registrationId) {
        return jdbc.update(
                        "UPDATE activity_registration SET participant_status='MISSING_ON_RETURN',missing_alert_requested=TRUE,"
                                + "version=version+1 WHERE id=? AND participant_status='DEPARTED'",
                        registrationId)
                == 1;
    }

    /** FR-045: đăng ký đang ở ngoài của người cao tuổi trong chuyến chưa kết thúc hẳn. */
    public List<Long> outsideRegistrations(long residentId) {
        return jdbc.queryForList(
                "SELECT r.id FROM activity_registration r JOIN activity_session s ON s.id=r.session_id"
                        + " WHERE r.resident_id=? AND r.participant_status IN ('DEPARTED','MISSING_ON_RETURN')"
                        + " AND s.trip_status IN ('IN_PROGRESS','RETURNED') FOR UPDATE",
                Long.class,
                residentId);
    }

    public void leftGroup(long registrationId, String reason) {
        jdbc.update(
                "UPDATE activity_registration SET participant_status='LEFT_GROUP',participant_reason=?,version=version+1"
                        + " WHERE id=? AND participant_status IN ('DEPARTED','MISSING_ON_RETURN')",
                reason,
                registrationId);
    }

    public Optional<Instant> departedAt(long registrationId) {
        return Optional.ofNullable(jdbc.queryForObject(
                        "SELECT MAX(e.occurred_at) FROM record_event e WHERE e.kind='M04.TRIP_ATTENDANCE'"
                                + " AND JSON_UNQUOTE(JSON_EXTRACT(e.content,'$.registrationId'))=?"
                                + " AND JSON_UNQUOTE(JSON_EXTRACT(e.content,'$.type'))='DEPARTURE'",
                        LocalDateTime.class,
                        Long.toString(registrationId)))
                .map(at -> at.toInstant(ZoneOffset.UTC)); // record_event lưu UTC (000 Rows)
    }

    /** Số lần đã ghi rời viện của đăng ký, kể cả lần đã Hủy ghi nhận (FR-031a), để khóa chống trùng của lần sau khác. */
    public int departureCount(long registrationId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM record_event e WHERE e.kind='M04.TRIP_ATTENDANCE'"
                        + " AND JSON_UNQUOTE(JSON_EXTRACT(e.content,'$.registrationId'))=?"
                        + " AND JSON_UNQUOTE(JSON_EXTRACT(e.content,'$.type'))='DEPARTURE'",
                Integer.class,
                Long.toString(registrationId));
    }

    /** FR-031a (b): người bị ghi đi nhầm trở về Được đi; chỉ khi chưa có bản ghi về. */
    public boolean undoDeparture(long registrationId) {
        return jdbc.update(
                        "UPDATE activity_registration SET participant_status='ELIGIBLE',participant_reason=NULL,"
                                + "missing_alert_requested=FALSE,version=version+1"
                                + " WHERE id=? AND participant_status IN ('DEPARTED','MISSING_ON_RETURN')",
                        registrationId)
                == 1;
    }

    /** Q-240: không còn ai đã rời viện thì chuyến quay về Đã lên lịch. */
    public boolean anyoneDeparted(long id) {
        return jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM activity_registration WHERE session_id=?"
                        + " AND participant_status IN ('DEPARTED','MISSING_ON_RETURN','RETURNED','LEFT_GROUP'))",
                Boolean.class,
                id);
    }

    public void unstart(long id) {
        jdbc.update(
                "UPDATE activity_session SET trip_status='SCHEDULED',trip_overdue_return=FALSE,version=version+1"
                        + " WHERE id=? AND trip_status='IN_PROGRESS'",
                id);
    }

    /** FR-043: mốc mới nên bỏ dấu quá giờ; mốc đã báo so theo giờ về dự kiến nên mốc mới được báo lại. */
    public void extend(long id, Instant plannedReturnAt) {
        jdbc.update(
                "UPDATE activity_session SET trip_planned_return_at=?,trip_overdue_return=FALSE,version=version+1 WHERE id=?",
                local(plannedReturnAt),
                id);
    }

    /** Đính chính lịch xe (019) đổi giờ rời, về dự kiến của chuyến Đã về; không đụng dấu quá giờ, mốc đã báo. */
    public void correctPlannedTimes(long id, Instant plannedDepartureAt, Instant plannedReturnAt) {
        jdbc.update(
                "UPDATE activity_session SET trip_planned_departure_at=?,trip_planned_return_at=?,version=version+1"
                        + " WHERE id=? AND trip_status='RETURNED'",
                local(plannedDepartureAt),
                local(plannedReturnAt),
                id);
    }

    public void finishReturn(long id, Instant at, Long staffId) {
        jdbc.update(
                "UPDATE activity_session SET trip_status='RETURNED',trip_return_finished_at=?,"
                        + "trip_return_finished_by_staff_id=?,version=version+1 WHERE id=? AND trip_status='IN_PROGRESS'",
                local(at),
                staffId,
                id);
    }

    private static String notifiedColumn(boolean manager) {
        return manager ? "trip_manager_notified_for" : "trip_overdue_notified_for";
    }

    /** FR-049 (manager=false), FR-049a (manager=true): chuyến Đang đi đã qua mốc mà mốc này chưa báo. */
    public List<Trip> overdue(Instant returnBefore, boolean manager) {
        return jdbc.query(
                "SELECT * FROM activity_session WHERE trip_status='IN_PROGRESS' AND trip_planned_return_at<=?"
                        + " AND NOT (" + notifiedColumn(manager) + " <=> trip_planned_return_at) ORDER BY id",
                ActivityTripStore::row,
                local(returnBefore));
    }

    /** Đánh dấu mốc đã báo; trả false nếu lượt khác vừa báo hoặc giờ về vừa được gia hạn. */
    public boolean claimOverdue(long id, Instant plannedReturnAt, boolean manager) {
        String column = notifiedColumn(manager);
        return jdbc.update(
                        "UPDATE activity_session SET " + column + "=trip_planned_return_at"
                                + (manager ? "" : ",trip_overdue_return=TRUE")
                                + " WHERE id=? AND trip_status='IN_PROGRESS' AND trip_planned_return_at=?"
                                + " AND NOT (" + column + " <=> trip_planned_return_at)",
                        id,
                        local(plannedReturnAt))
                == 1;
    }

    public List<Trip> active() {
        return jdbc.query(
                "SELECT * FROM activity_session WHERE trip_status IN ('SCHEDULED','IN_PROGRESS') ORDER BY id",
                ActivityTripStore::row);
    }

    public Optional<Instant> firstDeparture(long tripId) {
        return Optional.ofNullable(jdbc.queryForObject(
                        "SELECT MIN(e.occurred_at) FROM record_event e WHERE e.kind='M04.TRIP_ATTENDANCE' AND JSON_UNQUOTE(JSON_EXTRACT(e.content,'$.tripId'))=? AND JSON_UNQUOTE(JSON_EXTRACT(e.content,'$.type'))='DEPARTURE'",
                        LocalDateTime.class,
                        Long.toString(tripId)))
                .map(at -> at.toInstant(ZoneOffset.UTC)); // record_event lưu UTC (000 Rows)
    }
}
