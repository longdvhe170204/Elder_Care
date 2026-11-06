package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.internal.session.service.SessionAttendanceReminder;
import com.eldercare.backend.care.internal.session.service.SessionDayEnd;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** 014 T015/T016: cột chuyến và người tham gia gộp vào hai bảng 014, giữ ràng buộc sau R-15. */
@Transactional
class T016_TripSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SessionDayEnd dayEnd;

    @Autowired
    SessionAttendanceReminder reminders;

    long session;
    LocalDateTime departure;

    @BeforeEach
    void fixture() {
        session = TestUsers.nextId();
        departure = time.today().plusDays(1).atTime(7, 0);
        jdbc.update(
                "INSERT INTO activity_session(id,activity_id,starts_at,ends_at,source,status,created_by,created_at)"
                        + " VALUES(?,?,?,?,'SINGLE','SCHEDULED',?,?)",
                session,
                TestUsers.nextId(),
                departure,
                departure.plusHours(6),
                TestUsers.nextId(),
                departure.minusDays(1));
    }

    @Test
    void FR_034_tripAndParticipantAreStoredOnTheExistingSessionAndRegistration() {
        trip(departure, departure.plusHours(6));
        long registration = register();
        jdbc.update("UPDATE activity_registration SET participant_status='ELIGIBLE' WHERE id=?", registration);
        assertThat(jdbc.queryForMap(
                        "SELECT trip_destination,trip_transport,trip_facility_vehicle,trip_status FROM activity_session WHERE id=?",
                        session))
                .containsAllEntriesOf(Map.of(
                        "trip_destination",
                        "Chùa",
                        "trip_transport",
                        "Xe thuê",
                        "trip_facility_vehicle",
                        false,
                        "trip_status",
                        "SCHEDULED"));
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE id=?", String.class, registration))
                .isEqualTo("ELIGIBLE");
    }

    @Test
    void FR_034_instituteSessionAndRegistrationDoNotRequireTripColumns() {
        long registration = register();
        assertThat(jdbc.queryForObject("SELECT trip_status FROM activity_session WHERE id=?", String.class, session))
                .isNull();
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE id=?", String.class, registration))
                .isNull();
    }

    @Test
    void FR_034_tripCannotBeScheduledWithoutDestinationAndTransport() {
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE activity_session SET trip_status='SCHEDULED',trip_planned_departure_at=?,"
                                + "trip_planned_return_at=?,trip_facility_vehicle=FALSE WHERE id=?",
                        departure,
                        departure.plusHours(6),
                        session))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint 'ck_activity_session_trip' is violated");
    }

    @Test
    void FR_034_plannedReturnMustBeAfterDeparture() {
        assertThatThrownBy(() -> trip(departure, departure))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint 'ck_activity_session_trip' is violated");
        assertThatThrownBy(() -> trip(departure, departure.minusMinutes(1)))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Check constraint 'ck_activity_session_trip' is violated");
    }

    @Test
    void FR_015_registrationUniquenessStillAppliesToTripParticipants() {
        trip(departure, departure.plusHours(6));
        long registration = register();
        jdbc.update("UPDATE activity_registration SET participant_status='REGISTERED' WHERE id=?", registration);
        long resident = jdbc.queryForObject(
                "SELECT resident_id FROM activity_registration WHERE id=?", Long.class, registration);
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO activity_registration(session_id,resident_id,source,registered_at,status,participant_status)"
                                + " VALUES(?,?,'MANUAL',?,'REGISTERED','REGISTERED')",
                        session,
                        resident,
                        departure.minusDays(1)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void FR_014a_dayEndDoesNotCloseAnUnfinishedTrip() {
        trip(departure, departure.plusHours(6));
        dayEnd.run(
                departure.toLocalDate(),
                departure.plusDays(1).atZone(BusinessTime.ZONE).toInstant());
        assertThat(jdbc.queryForObject("SELECT status FROM activity_session WHERE id=?", String.class, session))
                .isEqualTo("SCHEDULED");
    }

    @Test
    void FR_030_tripDoesNotReceiveAnInstituteSessionAttendanceReminder() {
        trip(departure, departure.plusHours(6));
        reminders.run(departure.plusDays(1).atZone(BusinessTime.ZONE).toInstant());
        assertThat(jdbc.queryForObject(
                        "SELECT attendance_reminded_at FROM activity_session WHERE id=?", LocalDateTime.class, session))
                .isNull();
    }

    private void trip(LocalDateTime from, LocalDateTime to) {
        jdbc.update(
                "UPDATE activity_session SET trip_destination='Chùa',trip_planned_departure_at=?,"
                        + "trip_planned_return_at=?,trip_transport='Xe thuê',trip_facility_vehicle=FALSE,"
                        + "trip_status='SCHEDULED' WHERE id=?",
                from,
                to,
                session);
    }

    private long register() {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO activity_registration(id,session_id,resident_id,source,registered_at,status)"
                        + " VALUES(?,?,?,'MANUAL',?,'REGISTERED')",
                id,
                session,
                TestUsers.nextId(),
                departure.minusDays(1));
        return id;
    }
}
