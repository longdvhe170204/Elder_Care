package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.AttendanceStatus;
import java.util.*;
import org.junit.jupiter.api.Test;

/** T027: bảng FR-013, không tự chuyển Có mặt thành Đã về khi hết ngày. */
class FR_013_AttendanceTransitionsTest {
    private Class<?> table() {
        try {
            return Class.forName("com.eldercare.backend.care.internal.attendance.domain.AttendanceTransitions");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Chưa cài bảng FR-013", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Optional<AttendanceStatus> next(AttendanceStatus status, String event) throws Exception {
        return (Optional<AttendanceStatus>)
                table().getMethod("next", AttendanceStatus.class, String.class).invoke(null, status, event);
    }

    @Test
    void FR_013_allEventStatePairsMatchTheTable() throws Exception {
        var expected = Map.of(
                "ARRIVE",
                        Map.of(
                                AttendanceStatus.NOT_ARRIVED,
                                AttendanceStatus.PRESENT,
                                AttendanceStatus.UNREPORTED_ABSENT,
                                AttendanceStatus.PRESENT,
                                AttendanceStatus.HOLIDAY_CANCELLED,
                                AttendanceStatus.PRESENT),
                "LEAVE", Map.of(AttendanceStatus.PRESENT, AttendanceStatus.LEFT),
                "HOLIDAY",
                        Map.of(
                                AttendanceStatus.NOT_ARRIVED,
                                AttendanceStatus.HOLIDAY_CANCELLED,
                                AttendanceStatus.UNREPORTED_ABSENT,
                                AttendanceStatus.HOLIDAY_CANCELLED),
                "REPORT_ABSENCE", Map.of(AttendanceStatus.NOT_ARRIVED, AttendanceStatus.REPORTED_ABSENT),
                "NO_SHOW", Map.of(AttendanceStatus.NOT_ARRIVED, AttendanceStatus.UNREPORTED_ABSENT));
        for (var event : expected.entrySet()) {
            for (var status : AttendanceStatus.values()) {
                assertThat(next(status, event.getKey()))
                        .as("%s từ %s", event.getKey(), status)
                        .isEqualTo(Optional.ofNullable(event.getValue().get(status)));
            }
        }
    }

    @Test
    void FR_019_endOfDayNeverChecksOutOrReopensARecord() throws Exception {
        for (var status : AttendanceStatus.values()) {
            assertThat(next(status, "DAY_ENDED")).isEmpty();
            assertThat(next(status, "UNKNOWN")).isEmpty();
        }
    }
}
