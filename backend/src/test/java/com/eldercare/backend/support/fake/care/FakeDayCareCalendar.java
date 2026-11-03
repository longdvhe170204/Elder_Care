package com.eldercare.backend.support.fake.care;

import com.eldercare.backend.care.api.spi.DayCareCalendarPort;
import java.time.LocalDate;
import java.util.*;

/** Bản giả duy nhất của SPI calendar005; mọi ngày phải được fixture đặt rõ. */
public class FakeDayCareCalendar implements DayCareCalendarPort {
    private final Map<String, CalendarView> days = new HashMap<>();

    public void day(long resident, LocalDate date, CalendarView value) {
        days.put(resident + ":" + date, value);
    }

    public CalendarView onDate(long resident, LocalDate date) {
        var value = days.get(resident + ":" + date);
        if (value == null) throw new IllegalStateException("Chưa đặt nguồn ngày nghỉ/báo vắng 004");
        return value;
    }

    public void reset() {
        days.clear();
    }
}
