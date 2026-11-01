package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.dto.TaskStatus;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** T018: bảng FR-026 không cho phép mở lại công việc đã đóng. */
class FR_026_CareTaskTransitionsTest {
    private Class<?> transitions() {
        try {
            return Class.forName("com.eldercare.backend.care.internal.task.domain.CareTaskTransitions");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Chưa cài bảng chuyển trạng thái công việc của T018", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<TaskStatus> next(TaskStatus before, String event, Importance importance) throws Exception {
        return (Optional<TaskStatus>) transitions()
                .getMethod("next", TaskStatus.class, String.class, Importance.class)
                .invoke(null, before, event, importance);
    }

    private TaskStatus initial(Instant from, Instant to, Instant now) throws Exception {
        try {
            return (TaskStatus) transitions()
                    .getMethod("initialStatus", Instant.class, Instant.class, Instant.class)
                    .invoke(null, from, to, now);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            throw e;
        }
    }

    @Test
    void FR_026_everyEventStatePairFollowsTheTable() throws Exception {
        var expected = Map.of(
                "WINDOW_OPENED", Map.of(TaskStatus.NOT_YET_DUE, TaskStatus.DUE),
                "WINDOW_EXPIRED", Map.of(TaskStatus.DUE, TaskStatus.OVERDUE),
                "CANCEL", Map.of(TaskStatus.NOT_YET_DUE, TaskStatus.CANCELLED),
                "SOURCE_CANCELLED", Map.of(TaskStatus.NOT_YET_DUE, TaskStatus.CANCELLED),
                "COMPLETE_IN_WINDOW",
                        Map.of(TaskStatus.DUE, TaskStatus.COMPLETED, TaskStatus.OVERDUE, TaskStatus.COMPLETED),
                "COMPLETE_LATE", Map.of(TaskStatus.OVERDUE, TaskStatus.COMPLETED_LATE),
                "DO_NOT_PERFORM",
                        Map.of(TaskStatus.DUE, TaskStatus.NOT_PERFORMED, TaskStatus.OVERDUE, TaskStatus.NOT_PERFORMED),
                "HANDOVER_CONFIRMED", Map.of(TaskStatus.OVERDUE, TaskStatus.NOT_PERFORMED),
                "RESIDENT_FINAL",
                        Map.of(
                                TaskStatus.NOT_YET_DUE,
                                TaskStatus.CANCELLED,
                                TaskStatus.DUE,
                                TaskStatus.NOT_PERFORMED,
                                TaskStatus.OVERDUE,
                                TaskStatus.NOT_PERFORMED));
        for (var event : expected.entrySet()) {
            for (var before : TaskStatus.values()) {
                assertThat(next(before, event.getKey(), Importance.NORMAL))
                        .as("%s từ %s", event.getKey(), before)
                        .isEqualTo(Optional.ofNullable(event.getValue().get(before)));
            }
        }
    }

    @Test
    void FR_047_handoverDoesNotCloseImportantOrMandatoryTasks() throws Exception {
        for (var importance : new Importance[] {Importance.IMPORTANT, Importance.MANDATORY}) {
            for (var status : TaskStatus.values()) {
                assertThat(next(status, "HANDOVER_CONFIRMED", importance)).isEmpty();
            }
        }
    }

    @Test
    void FR_026_unknownEventDoesNotChangeAnyState() throws Exception {
        for (var status : TaskStatus.values()) {
            assertThat(next(status, "UNKNOWN", Importance.NORMAL)).isEmpty();
        }
    }

    @Test
    void BR_M04_26_creationUsesTheCurrentClockAndBothWindowBoundaries() throws Exception {
        var from = Instant.parse("2026-11-01T01:00:00Z");
        var to = from.plusSeconds(1800);
        assertThat(initial(from, to, from.minusNanos(1))).isEqualTo(TaskStatus.NOT_YET_DUE);
        assertThat(initial(from, to, from)).isEqualTo(TaskStatus.DUE);
        assertThat(initial(from, to, from.plusSeconds(900))).isEqualTo(TaskStatus.DUE);
        assertThat(initial(from, to, to)).isEqualTo(TaskStatus.DUE);
        assertThat(initial(from, to, to.plusNanos(1))).isEqualTo(TaskStatus.OVERDUE);
        assertThat(initial(from, from, from)).isEqualTo(TaskStatus.DUE);
    }

    @Test
    void BR_M04_26_reversedWindowCannotProduceAState() {
        var from = Instant.parse("2026-11-01T01:00:00Z");
        assertThatThrownBy(() -> initial(from, from.minusSeconds(1), from))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
