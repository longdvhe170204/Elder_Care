package com.eldercare.backend.care.internal.attendance.domain;

import com.eldercare.backend.care.api.spi.DayCareCalendarPort.CalendarView;
import java.time.LocalTime;

public record AttendanceDaySource(LocalTime arrival, LocalTime departure, CalendarView calendar) {
    public boolean scheduled() {
        return arrival != null && !calendar.holiday();
    }
}
