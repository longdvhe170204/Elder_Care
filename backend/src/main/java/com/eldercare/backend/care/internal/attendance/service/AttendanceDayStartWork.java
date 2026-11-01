package com.eldercare.backend.care.internal.attendance.service;

import com.eldercare.backend.care.internal.attendance.persistence.AttendanceStore;
import com.eldercare.backend.care.internal.scheduling.service.CareDailyWork;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.*;
import java.util.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** FR-012: xác định trạng thái từ lịch đã có; không ghi điểm danh hoặc công việc. */
@Component
@Order(10)
public class AttendanceDayStartWork implements CareDailyWork {
    private final AttendanceSources sources;
    private final AttendanceService commands;
    private final AttendanceStore store;

    public AttendanceDayStartWork(AttendanceSources sources, AttendanceService commands, AttendanceStore store) {
        this.sources = sources;
        this.commands = commands;
        this.store = store;
    }

    public String name() {
        return "DAY_CARE_DAY_START";
    }

    public List<LocalDate> dueDates(LocalDate today) {
        return sources.dueDates(today);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int run(LocalDate date, Instant now) {
        if (date.isAfter(now.atZone(BusinessTime.ZONE).toLocalDate())) return 0;
        int count = 0;
        for (long resident : sources.currentResidents(date)) {
            int before = store.find(resident, date).map(r -> r.version()).orElse(-1);
            commands.refresh(resident, date);
            if (store.find(resident, date).map(r -> r.version()).orElse(-1) != before) count++;
        }
        return count;
    }
}
