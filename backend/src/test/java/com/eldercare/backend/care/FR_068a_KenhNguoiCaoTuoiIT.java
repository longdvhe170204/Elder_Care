package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.api.ResidentActivityReadApi;
import com.eldercare.backend.care.api.ResidentTripReadApi;
import com.eldercare.backend.care.internal.trip.domain.TripDuties;
import com.eldercare.backend.care.internal.trip.persistence.ActivityTripStore;
import com.eldercare.backend.care.internal.trip.service.TripPreparationService;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

@Tag("BR-78")
class FR_068a_KenhNguoiCaoTuoiIT extends CareResultTestBase {
    @Autowired
    ApplicationContext context;

    @Autowired
    ActivityTripStore trips;

    @Autowired
    TripPreparationService preparation;

    @Autowired
    com.eldercare.backend.common.time.TestClockControl clock;

    @Autowired
    com.eldercare.backend.care.internal.session.service.ResidentSessionReminders reminders;

    @Autowired
    com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore sessions;

    @Test
    void FR_068a_H1_H2_readsUpcomingSessionsAndCurrentTripThroughCareApi() throws Exception {
        com.eldercare.backend.support.TableRows.each(List.of("H1", "H2", "H3"), row -> {
            if (row.equals("H1")) upcoming();
            else if (row.equals("H2")) ongoing();
            else FR_071_remindsTheResidentOnceForTheDayAndForSameDayRegistration();
        });
    }

    private void upcoming() {
        var f = new SessionAttendanceFixture(jdbc, time, assignments, user, residents);
        beds.release(resident).dayCare(resident, f.floor);
        long a = f.activity(false);
        long own = f.session(a, Duration.ofHours(1));
        long facility = f.session(a, Duration.ofHours(2));
        long other = f.session(a, Duration.ofHours(3));
        long cancelled = f.session(a, Duration.ofHours(4));
        jdbc.update("UPDATE activity_session SET floor_area_id=? WHERE id=?", TestUsers.nextId(), other);
        jdbc.update(
                "UPDATE activity_session SET status='CANCELLED',cancel_reason='Kiểm thử',cancelled_by='STAFF' WHERE id=?",
                cancelled);
        long facilityActivity = f.activity(false);
        jdbc.update(
                "UPDATE simple_record SET content=JSON_SET(content,'$.scope','FACILITY') WHERE id=?", facilityActivity);
        jdbc.update(
                "UPDATE activity_session SET activity_id=?,floor_area_id=NULL WHERE id=?", facilityActivity, facility);
        f.registration(own, resident);
        var sessions = context
                .getBean(ResidentActivityReadApi.class)
                .upcoming(resident, time.today(), time.today().plusDays(1))
                .stream()
                .filter(s -> java.util.Set.of(own, facility, other, cancelled).contains(s.sessionId()))
                .toList();
        assertThat(sessions).extracting(v -> v.sessionId()).containsExactly(own, facility);
        assertThat(sessions.getFirst().registered()).isTrue();
        assertThat(sessions.getLast().registered()).isFalse();
        assertThat(sessions.getLast().facilityWide()).isTrue();
    }

    private void ongoing() {
        var f = new SessionAttendanceFixture(jdbc, time, assignments, user, residents);
        long a = f.activity(false);
        long trip = f.session(a, Duration.ofHours(-1));
        trips.initialize(
                trip,
                new com.eldercare.backend.care.internal.trip.domain.TripPlan(
                        "Công viên", time.now().minusSeconds(3600), time.now().plusSeconds(3600), "Xe thuê", false));
        jdbc.update("UPDATE activity_session SET trip_status='IN_PROGRESS' WHERE id=?", trip);
        long registration = f.registration(trip, resident);
        trips.participant(registration, "DEPARTED", null);
        long oldLeader = TestUsers.nextId(), currentLeader = TestUsers.nextId(), companion = TestUsers.nextId();
        preparation.replaceDuties(
                trips.find(trip).orElseThrow(),
                new TripDuties(oldLeader, List.of(currentLeader, companion)),
                "ASSIGN",
                null);
        preparation.replaceDuties(
                trips.find(trip).orElseThrow(),
                new TripDuties(currentLeader, List.of(oldLeader, companion)),
                "TRANSFER_LEADER",
                null);
        var api = context.getBean(ResidentTripReadApi.class);
        assertThat(api.ongoing(resident)).hasValueSatisfying(view -> {
            assertThat(view.tripId()).isEqualTo(trip);
            assertThat(view.leaderId()).contains(currentLeader);
            assertThat(view.companionIds()).containsExactly(oldLeader, companion);
        });
        trips.participant(registration, "RETURNED", null);
        assertThat(api.ongoing(resident)).isEmpty();
    }

    private void FR_071_remindsTheResidentOnceForTheDayAndForSameDayRegistration() {
        clock.setTo(time.startOfDay(time.today()).plusSeconds(36000));
        try {
            var f = new SessionAttendanceFixture(jdbc, time, assignments, user, residents);
            long a = f.activity(false),
                    daily = f.session(a, Duration.ofHours(1)),
                    later = f.session(a, Duration.ofHours(2));
            long r = f.resident();
            new TestUsers(jdbc, time).resident(r);
            long registration = f.registration(daily, r);
            reminders.run(time.today(), time.now());
            reminders.run(time.today(), time.now());
            long laterRegistration = f.registration(later, r);
            reminders.registered(sessions.find(later).orElseThrow(), laterRegistration, r);
            reminders.registered(sessions.find(later).orElseThrow(), laterRegistration, r);
            assertThat(events.stream(com.eldercare.backend.care.api.events.ActivityResidentReminder.class)
                            .filter(e -> e.residentId() == r))
                    .extracting(e -> e.registrationId())
                    .containsExactlyInAnyOrder(registration, laterRegistration);
        } finally {
            clock.reset();
        }
    }
}
