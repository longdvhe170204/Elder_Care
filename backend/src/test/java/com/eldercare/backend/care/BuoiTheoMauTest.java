package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.internal.activity.domain.TemplateVersion;
import com.eldercare.backend.care.internal.session.domain.SessionTemplate;
import com.eldercare.backend.care.internal.session.domain.SessionTemplate.VersionFrom;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 014 T007 (FR-005, FR-007, FR-009; research L-04 {@code BuoiTheoMau}): thời điểm bắt đầu theo phiên bản mẫu. */
class BuoiTheoMauTest {
    // 05/10/2026 là thứ 2.
    static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    static TemplateVersion weekdays(String start, String end, LocalDate startDate, LocalDate endDate) {
        return new TemplateVersion(List.of(1, 2, 3, 4, 5), null, start, end, startDate, endDate, "r", 1);
    }

    static Instant at(LocalDate day, String time) {
        return day.atTime(LocalTime.parse(time)).atZone(BusinessTime.ZONE).toInstant();
    }

    @Test
    void FR_005_weekdayTemplateGivesOneSlotPerMatchingDay() {
        var slots = SessionTemplate.slots(
                List.of(new VersionFrom(MONDAY, weekdays("08:00", "08:45", MONDAY, null))), MONDAY, MONDAY.plusDays(6));
        assertThat(slots).hasSize(5);
        assertThat(slots.getFirst().startsAt()).isEqualTo(at(MONDAY, "08:00"));
        assertThat(slots.getFirst().endsAt()).isEqualTo(at(MONDAY, "08:45"));
        assertThat(slots.getLast().startsAt()).isEqualTo(at(MONDAY.plusDays(4), "08:00"));
    }

    @Test
    void FR_009_eachDayUsesTheVersionInEffectOnThatDay() {
        var versions = List.of(
                new VersionFrom(MONDAY, weekdays("08:00", "08:45", MONDAY, null)),
                new VersionFrom(MONDAY.plusDays(3), weekdays("08:30", "09:15", MONDAY, null)));
        var slots = SessionTemplate.slots(versions, MONDAY, MONDAY.plusDays(4));
        assertThat(slots)
                .extracting(SessionTemplate.Slot::startsAt)
                .containsExactly(
                        at(MONDAY, "08:00"),
                        at(MONDAY.plusDays(1), "08:00"),
                        at(MONDAY.plusDays(2), "08:00"),
                        at(MONDAY.plusDays(3), "08:30"),
                        at(MONDAY.plusDays(4), "08:30"));
    }

    @Test
    void FR_006_noVersionNoSlot() {
        assertThat(SessionTemplate.slots(List.of(), MONDAY, MONDAY.plusDays(6))).isEmpty();
    }
}
