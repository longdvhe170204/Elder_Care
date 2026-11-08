package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.common.notification.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class NFR_04_CareEventsTest {
    @Test
    void NFR_04_legacyBatchedReminderOutboxIsReadableWithoutReserializingIds() {
        var context = new com.eldercare.backend.care.api.dto.CareEventContext(
                "M04.TASK", 7, "BATCHED_REMINDER", AT, Optional.of(2L), List.of(new RecipientGroup.Staff(4, "TEST")));
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        String legacy = json.writeValueAsString(Map.of("taskIds", List.of(7L, 8L), "context", context));
        var replay = json.readValue(legacy, com.eldercare.backend.care.api.events.BatchedOverdueTaskReminder.class);
        assertThat(replay.floorAreaId()).isEqualTo(2);
        assertThat(replay.taskCount()).isEqualTo(2);
        assertThat(json.readTree(json.writeValueAsString(replay)).has("taskIds"))
                .isFalse();
        assertThat(replay.eventKey())
                .isEqualTo(
                        new com.eldercare.backend.care.api.events.BatchedOverdueTaskReminder(List.of(7L, 8L), context)
                                .eventKey());
    }

    @Test
    void NFR_04_legacyResidentAlertOutboxIsReadableWithoutSourceDetails() {
        var context = new com.eldercare.backend.care.api.dto.CareEventContext(
                "M04.TASK", 7, "MANDATORY", AT, Optional.of(2L), List.of());
        var legacy = new com.eldercare.backend.care.api.events.AlertRequested(
                "OVERDUE_CARE_TASK",
                NotificationLevel.MEDIUM,
                8,
                "M04.TASK:7",
                com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType.MEAL,
                9,
                context);
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        String payload = json.writeValueAsString(legacy).replace(",\"sourceDetails\":null", "");
        var replay = json.readValue(payload, com.eldercare.backend.care.api.events.AlertRequested.class);
        assertThat(replay.residentId()).isEqualTo(8);
        assertThat(replay.sourceDetails()).isEmpty();
        assertThat(replay.eventKey()).isEqualTo(legacy.eventKey());
    }

    private static final Instant AT = Instant.parse("2026-10-27T01:00:00Z");

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
                    if (t == long.class || t == Long.class) return 7L;
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
