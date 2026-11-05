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
                .map(at -> at.atZone(BusinessTime.ZONE).toInstant());
    }
}
