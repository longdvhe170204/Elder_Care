package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.notification.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class NFR_04_CareEventsTest {
    @Test
    void NFR_04_sameIdAndMilestoneInDifferentSourceNamespacesHaveDifferentKeys() {
        var a = new com.eldercare.backend.care.api.dto.CareEventContext(
                "M08.MEAL", 7, "version:2", AT, Optional.empty(), List.of());
        var b = new com.eldercare.backend.care.api.dto.CareEventContext(
                "M06.MEASUREMENT", 7, "version:2", AT, Optional.empty(), List.of());
        assertThat(new com.eldercare.backend.care.api.events.TaskSourceChanged(
                                com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType.MEAL, 7, 1, a)
                        .eventKey())
                .isNotEqualTo(new com.eldercare.backend.care.api.events.TaskSourceChanged(
                                com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType.MEAL, 7, 1, b)
                        .eventKey());
    }

    @Test
    void NFR_04_sourceKindAndAgreementReviewKindAlsoSeparateEventKeys() {
        var context = new com.eldercare.backend.care.api.dto.CareEventContext(
                "M04.SOURCE", 7, "version:2", AT, Optional.empty(), List.of());
        assertThat(new com.eldercare.backend.care.api.events.TaskSourceChanged(
                                com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType.MEAL, 7, 1, context)
                        .eventKey())
                .isNotEqualTo(new com.eldercare.backend.care.api.events.TaskSourceChanged(
                                com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType.MEASUREMENT, 7, 1, context)
                        .eventKey());
    }

    @Test
    void NFR_04_agreementReviewKindsHaveSeparateKeys() {
        var context = new com.eldercare.backend.care.api.dto.CareEventContext(
                "M04.SOURCE", 7, "version:2", AT, Optional.empty(), List.of());
        assertThat(new com.eldercare.backend.care.api.events.RiskAgreementReviewReminder(7, "AGREEMENT", context)
                        .eventKey())
                .isNotEqualTo(
                        new com.eldercare.backend.care.api.events.RiskAgreementReviewReminder(7, "ASSESSMENT", context)
                                .eventKey());
    }

    private static final Instant AT = Instant.parse("2026-10-27T01:00:00Z");

    @Test
    void NFR_04_allPublishedEventsHaveStableMilestoneKeysAndExpectedLevels() throws Exception {
        Map<String, NotificationLevel> notices = new LinkedHashMap<>();
        for (String name : List.of(
                "OverdueTaskReminder",
                "BatchedOverdueTaskReminder",
                "TaskPlanningNeeded",
                "CarePlanChanged",
                "TaskSourceChanged",
                "CarePlanSubmitted",
                "CarePlanReturned",
                "CarePlanApprovalWithdrawn",
                "CarePlanReviewNeeded",
                "DayCareCheckedIn",
                "UnreportedAbsence",
                "RiskAgreementReviewReminder")) notices.put(name, NotificationLevel.LOW);
        for (String name : List.of(
                "OverdueTaskNeedsHandling",
                "ShiftWithoutTasks",
                "CarePlanApprovalOverdue",
                "CarePlanManagerEscalation",
                "TaskBecameShared",
                "SharedTaskWithoutQualifiedStaff")) notices.put(name, NotificationLevel.MEDIUM);
        var contextType = Class.forName("com.eldercare.backend.care.api.dto.CareEventContext");
        Object context = contextType.getConstructors()[0].newInstance(
                "M04.TASK", 7L, "due:2026-10-27", AT, Optional.of(2L), List.of(new RecipientGroup.Staff(4, "TEST")));
        Object later = contextType.getConstructors()[0].newInstance(
                "M04.TASK",
                7L,
                "due:2026-10-27",
                AT.plusSeconds(60),
                Optional.of(2L),
                List.of(new RecipientGroup.Staff(4, "TEST")));
        List<String> all = new ArrayList<>(notices.keySet());
        all.addAll(List.of(
                "AlertRequested",
                "AlertClosureRequested",
                "AttendanceDetermined",
                "AttendanceChanged",
                "BillableCareResultRecorded",
                "BillableCareResultCorrected",
                "ProlongedPoorEating",
                "MeasurementTaskMissed",
                "SleepResultRecorded"));
        for (String name : all) {
            var event = event(name, context);
            var replay = event(name, later);
            String key = (String) event.getClass().getMethod("eventKey").invoke(event);
            assertThat(key).startsWith(name + ":7:").hasSizeLessThan(200);
            assertThat(event.getClass().getMethod("eventKey").invoke(replay)).isEqualTo(key);
            if (notices.containsKey(name)) {
                var requests = ((HasNotificationRequests) event).notificationRequests();
                assertThat(requests).hasSize(1);
                assertThat(requests.getFirst().level()).contains(notices.get(name));
                assertThat(requests.getFirst().eventKey()).isEqualTo(key);
                assertThat(requests.getFirst().sourceModule()).isEqualTo("M04");
                assertThat(requests.getFirst().occurredAt()).isEqualTo(AT);
                assertThat(requests.getFirst().recipientGroups()).containsExactly(new RecipientGroup.Staff(4, "TEST"));
            }
        }
    }

    @Test
    void FR_019_dayEndUsesMediumForShiftLeadAndLowForFloorManager() throws Exception {
        var type = Class.forName("com.eldercare.backend.care.api.dto.CareEventContext");
        Object context = type.getConstructors()[0].newInstance(
                "M04.DAY_CARE_ATTENDANCE",
                7L,
                "2026-10-27",
                AT,
                Optional.of(2L),
                List.of(
                        new RecipientGroup.ShiftLead(2, new Substitution.Default()),
                        new RecipientGroup.FloorManager(2, new Substitution.Default())));
        var requests = ((HasNotificationRequests) event("DayCarePresentAtDayEnd", context)).notificationRequests();
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).level()).contains(NotificationLevel.MEDIUM);
        assertThat(requests.get(0).recipientGroups())
                .containsExactly(new RecipientGroup.ShiftLead(2, new Substitution.Default()));
        assertThat(requests.get(1).level()).contains(NotificationLevel.LOW);
        assertThat(requests.get(1).recipientGroups())
                .containsExactly(new RecipientGroup.FloorManager(2, new Substitution.Default()));
    }

    private Object event(String name, Object context) throws Exception {
        var type = Class.forName("com.eldercare.backend.care.api.events." + name);
        var constructor = type.getConstructors()[0];
        Object[] arguments = Arrays.stream(constructor.getParameterTypes())
                .map(t -> {
                    if (t.isInstance(context)) return context;
                    if (t == long.class) return 7L;
                    if (t == boolean.class) return false;
                    if (t == int.class) return 1;
                    if (t == String.class) return "TEST";
                    if (t == Instant.class) return AT;
                    if (t == LocalDate.class) return LocalDate.of(2026, 10, 27);
                    if (t == LocalTime.class) return LocalTime.NOON;
                    if (t == BigDecimal.class) return BigDecimal.ONE;
                    if (t == List.class) return List.of(7L);
                    if (t == Set.class) return Set.of(7L);
                    if (t == Optional.class) return Optional.empty();
                    if (t.isEnum()) return t.getEnumConstants()[0];
                    throw new IllegalArgumentException(t.getName());
                })
                .toArray();
        return constructor.newInstance(arguments);
    }
}
