package com.eldercare.backend.support.fake.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.api.dto.ActivityAttendanceTraceView;
import com.eldercare.backend.incident.api.dto.AlertShiftView;
import com.eldercare.backend.incident.api.dto.EmergencyFamilyContactView;
import com.eldercare.backend.incident.api.dto.IncidentNotificationView;
import com.eldercare.backend.incident.api.dto.VisitTracePersonView;
import com.eldercare.backend.incident.api.dto.VisitTraceView;
import com.eldercare.backend.incident.api.spi.AttendanceTracePort;
import com.eldercare.backend.incident.api.spi.EmergencyFamilyPort;
import com.eldercare.backend.incident.api.spi.NotificationLogPort;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class T002_IncidentPortsTest {

    private static final Instant FROM = Instant.parse("2026-10-31T07:00:00Z");
    private static final Instant TO = FROM.plusSeconds(3600);

    @Test
    void FR_047_unconfiguredSourcesDoNotInventContactsOrTraceData() {
        var family = new FakeEmergencyFamily();
        var attendance = new FakeAttendanceTrace();
        var notifications = new FakeNotificationLog();

        assertThat(family.primaryContact(101)).isEmpty();
        assertThat(family.representatives(101)).isEmpty();
        assertThat(family.visitsInRange(101, FROM, TO)).isEmpty();
        assertThat(attendance.coAttendance(101, FROM, TO)).isEmpty();
        assertThat(notifications.bySource("M05.INCIDENT", 201)).isEmpty();
    }

    @Test
    void FR_050b_representativesKeepTheConfiguredCallOrderAndResidentScope() {
        var first = new EmergencyFamilyContactView(301, "Liên hệ thử một", Optional.of("0900000001"));
        var second = new EmergencyFamilyContactView(302, "Liên hệ thử hai", Optional.empty());
        var ordered = new ArrayList<>(List.of(second, first));
        var family = new FakeEmergencyFamily().primaryContact(101, first).representatives(101, ordered);
        ordered.clear();

        assertThat(family.primaryContact(101)).contains(first);
        assertThat(family.representatives(101)).containsExactly(second, first);
        assertThat(family.primaryContact(102)).isEmpty();
        assertThat(family.representatives(102)).isEmpty();
    }

    @Test
    void FR_060_visitsIncludeOverlapsAndOngoingVisitsButExcludeTouchingBoundaries() {
        var visitor = new VisitTracePersonView(
                Optional.of(301L), "Người thăm thử", Optional.of("CHILD"), Optional.empty(), true);
        var companion = new VisitTracePersonView(
                Optional.empty(), "Người đi cùng thử", Optional.empty(), Optional.empty(), false);
        var overlapping = new VisitTraceView(401, FROM.minusSeconds(60), Optional.of(TO), List.of(visitor, companion));
        var ongoing = new VisitTraceView(402, FROM, Optional.empty(), List.of(visitor));
        var endedAtStart = new VisitTraceView(403, FROM.minusSeconds(120), Optional.of(FROM), List.of(visitor));
        var startsAtEnd = new VisitTraceView(404, TO, Optional.empty(), List.of(visitor));
        var values = new ArrayList<>(List.of(overlapping, ongoing, endedAtStart, startsAtEnd));
        var family = new FakeEmergencyFamily().visits(101, values);
        values.clear();

        assertThat(family.visitsInRange(101, FROM, TO)).containsExactly(overlapping, ongoing);
        assertThat(family.visitsInRange(102, FROM, TO)).isEmpty();
        assertThat(overlapping.people()).containsExactly(visitor, companion);
        assertThat(companion.hasNotificationChannel()).isFalse();
    }

    @Test
    void FR_060_activityTraceFiltersByResidentAndOverlappingTime() {
        var overlapping = new ActivityAttendanceTraceView(501, FROM.minusSeconds(60), TO, List.of(102L, 103L));
        var endedAtStart = new ActivityAttendanceTraceView(502, FROM.minusSeconds(120), FROM, List.of(104L));
        var startsAtEnd = new ActivityAttendanceTraceView(503, TO, TO.plusSeconds(60), List.of(105L));
        var values = new ArrayList<>(List.of(overlapping, endedAtStart, startsAtEnd));
        var attendance = new FakeAttendanceTrace().sessions(101, values);
        values.clear();

        assertThat(attendance.coAttendance(101, FROM, TO)).containsExactly(overlapping);
        assertThat(attendance.coAttendance(102, FROM, TO)).isEmpty();
    }

    @Test
    void FR_040_notificationHistoryKeepsSourceTypeAndIdSeparate() {
        var row = new IncidentNotificationView("STAFF", 601, "IN_APP", Optional.of(FROM), Optional.of(TO));
        var rows = new ArrayList<>(List.of(row));
        var notifications = new FakeNotificationLog().source("M05.INCIDENT", 201, rows);
        rows.clear();

        assertThat(notifications.bySource("M05.INCIDENT", 201)).containsExactly(row);
        assertThat(notifications.bySource("M05.ALERT", 201)).isEmpty();
        assertThat(notifications.bySource("M05.INCIDENT", 202)).isEmpty();
        assertThat(notifications.bySource("M05.INCIDENT", 201).getFirst().viewedAt())
                .contains(TO);
    }

    @Test
    void Q_388_notificationHistoryKeepsTheFullCallChain() {
        var first = new IncidentNotificationView.CallReminderSummary(
                1,
                "FAMILY",
                601,
                "RESULT_RECORDED",
                List.of(new IncidentNotificationView.CallOutcome("NOT_REACHED", FROM)));
        var next = new IncidentNotificationView.CallReminderSummary(
                2, "FAMILY", 602, "RESULT_RECORDED", List.of(new IncidentNotificationView.CallOutcome("REACHED", TO)));
        var chain = new ArrayList<>(List.of(first, next));
        var row = new IncidentNotificationView("FAMILY", 601, "IN_APP", Optional.of(FROM), Optional.empty(), chain);
        var notifications = new FakeNotificationLog().source("M05.INCIDENT", 201, List.of(row));
        chain.clear();

        var notification = notifications.bySource("M05.INCIDENT", 201).getFirst();
        assertThat(notification.recipientId()).isEqualTo(601);
        assertThat(notification.callReminders()).containsExactly(first, next);
        assertThat(next.results()).containsExactly(new IncidentNotificationView.CallOutcome("REACHED", TO));
    }

    @Test
    void FR_031_existingStaffFakeUsesInclusiveStartAndExclusiveEnd() {
        var staff = new FakeAlertStaff()
                .shift(new AlertShiftView(701, 801L, FROM, TO), List.of(601L), List.of(602L))
                .nurse(101, 601);

        assertThat(staff.shiftAt(801, FROM)).isPresent();
        assertThat(staff.assignedNurse(101, FROM)).contains(601L);
        assertThat(staff.onDutyDoctors(FROM)).containsExactly(602L);
        assertThat(staff.shiftAt(801, TO)).isEmpty();
        assertThat(staff.assignedNurse(101, TO)).isEmpty();
        assertThat(staff.onDutyDoctors(TO)).isEmpty();
    }

    @Test
    void T002_sharedConfigProvidesTheSameFakeInstancesThroughTheirPorts() {
        try (var context = new AnnotationConfigApplicationContext(FakeM05Config.class)) {
            assertThat(context.getBean(EmergencyFamilyPort.class)).isSameAs(context.getBean(FakeEmergencyFamily.class));
            assertThat(context.getBean(AttendanceTracePort.class)).isSameAs(context.getBean(FakeAttendanceTrace.class));
            assertThat(context.getBean(NotificationLogPort.class)).isSameAs(context.getBean(FakeNotificationLog.class));
        }
    }
}
