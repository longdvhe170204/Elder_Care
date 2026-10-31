package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** T010: chuỗi mốc thuần,không cần database hoặc nguồn ca của008. */
class BR_M04_25_CarePlanItemSequenceTest {
    static CarePlanItemView item(
            long id,
            String content,
            String frequency,
            Integer hours,
            LocalTime start,
            List<LocalTime> times,
            Integer weekdays) {
        return new CarePlanItemView(
                id,
                41,
                content,
                frequency,
                Optional.ofNullable(hours),
                Optional.of(1),
                Optional.ofNullable(start),
                times,
                Optional.ofNullable(weekdays),
                15,
                15,
                Role.CAREGIVER,
                ResultType.PERFORMED,
                Importance.NORMAL,
                false,
                Optional.empty());
    }

    static Instant at(String local) {
        return LocalDateTime.parse(local).atZone(BusinessTime.ZONE).toInstant();
    }

    @SuppressWarnings("unchecked")
    private List<Instant> sequence(CarePlanItemView item, String from, String to) throws Exception {
        return (List<Instant>) Class.forName("com.eldercare.backend.care.internal.careplan.domain.CarePlanItemSequence")
                .getMethod("withinShift", CarePlanItemView.class, Instant.class, Instant.class)
                .invoke(null, item, at(from), at(to));
    }

    @Test
    void BR_M04_25_everyNHoursResetsAtMidnightInsteadOfContinuingYesterday() throws Exception {
        var row = item(1, "Đổi tư thế", "EVERY_N_HOURS", 5, LocalTime.of(1, 0), List.of(), null);
        assertThat(sequence(row, "2026-10-31T22:00", "2026-11-01T06:00")).containsExactly(at("2026-11-01T01:00"));
    }

    @Test
    void BR_M04_25_explicitTimesRespectHalfOpenShiftBoundaries() throws Exception {
        var row = item(
                1,
                "Hỗ trợ",
                "TIMES_PER_DAY",
                null,
                null,
                List.of(LocalTime.of(6, 0), LocalTime.NOON, LocalTime.of(18, 0)),
                null);
        assertThat(sequence(row, "2026-10-31T06:00", "2026-10-31T18:00"))
                .containsExactly(at("2026-10-31T06:00"), at("2026-10-31T12:00"));
    }

    @Test
    void BR_M04_25_weekdayUsesTheDateOfTheOccurrenceAcrossMidnight() throws Exception {
        var row = item(1, "Theo ngày", "DAYS_OF_WEEK", null, LocalTime.of(1, 0), List.of(), 1 << 6);
        assertThat(sequence(row, "2026-10-31T22:00", "2026-11-01T06:00")).containsExactly(at("2026-11-01T01:00"));
        assertThat(sequence(row, "2026-10-30T22:00", "2026-10-31T06:00")).isEmpty();
    }

    @Test
    void BR_M04_25_oncePerShiftHasOneRepresentativeAtTheShiftStart() throws Exception {
        var row = item(1, "Trong ca", "ONCE_PER_SHIFT", null, null, List.of(), null);
        assertThat(sequence(row, "2026-10-31T22:00", "2026-11-01T06:00")).containsExactly(at("2026-10-31T22:00"));
    }

    @Test
    void BR_M04_25_invalidIntervalCannotLoopOrProduceOccurrences() {
        var row = item(1, "Sai", "EVERY_N_HOURS", 0, LocalTime.of(1, 0), List.of(), null);
        assertThatThrownBy(() -> sequence(row, "2026-10-31T00:00", "2026-11-01T00:00"))
                .hasRootCauseInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
    }
}
