package com.eldercare.backend.care.internal.attendance.service;

import com.eldercare.backend.care.internal.attendance.persistence.AttendanceStore;
import com.eldercare.backend.care.internal.scheduling.service.CareDailyWork;
import java.time.*;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class AttendanceDayEndWork implements CareDailyWork {
    private final AttendanceStore store;
    private final AttendanceClockWork clock;

    public AttendanceDayEndWork(AttendanceStore store, AttendanceClockWork clock) {
        this.store = store;
        this.clock = clock;
    }

    public String name() {
        return "DAY_CARE_DAY_END";
    }

    public List<LocalDate> dueDates(LocalDate today) {
        return store.all().stream()
                .filter(r -> r.status().equals("PRESENT") && r.effectiveFrom().isBefore(today))
                .map(r -> r.effectiveFrom())
                .distinct()
                .sorted()
                .toList();
    }

    public int run(LocalDate date, Instant now) {
        return clock.endDay(date, now);
    }
}
