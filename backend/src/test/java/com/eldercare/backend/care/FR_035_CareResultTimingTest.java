package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.TaskStatus;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** T032: thời điểm thực hiện quyết định hoàn thành đúng khung hay trễ. */
class FR_035_CareResultTimingTest {
    private static final Instant FROM = Instant.parse("2026-11-01T02:45:00Z");
    private static final Instant TO = Instant.parse("2026-11-01T03:15:00Z");

    private TaskStatus completed(Instant from, Instant to, Instant performedAt) throws Exception {
        try {
            return (TaskStatus) Class.forName("com.eldercare.backend.care.internal.result.domain.CareResultTiming")
                    .getMethod("completedStatus", Instant.class, Instant.class, Instant.class)
                    .invoke(null, from, to, performedAt);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            throw e;
        }
    }

    @Test
    void FR_035_performedInsideWindowIsCompleted() throws Exception {
        assertThat(completed(FROM, TO, Instant.parse("2026-11-01T03:00:00Z"))).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void FR_035_bothWindowBoundariesAreInclusive() throws Exception {
        assertThat(completed(FROM, TO, FROM)).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completed(FROM, TO, TO)).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void FR_035_firstMicrosecondAfterWindowIsCompletedLate() throws Exception {
        assertThat(completed(FROM, TO, TO.plusNanos(1_000))).isEqualTo(TaskStatus.COMPLETED_LATE);
    }

    @Test
    void FR_035_zeroLengthWindowAcceptsItsExactInstant() throws Exception {
        assertThat(completed(FROM, FROM, FROM)).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void FR_035_performingBeforeWindowIsRejected() {
        assertThatThrownBy(() -> completed(FROM, TO, FROM.minusNanos(1_000)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void FR_035_reversedWindowIsRejected() {
        assertThatThrownBy(() -> completed(TO, FROM, TO)).isInstanceOf(IllegalArgumentException.class);
    }
}
