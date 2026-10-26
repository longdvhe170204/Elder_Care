package com.cambae.backend.support.fake.m02;

import com.cambae.backend.m02.api.ContractReadApi;
import com.cambae.backend.m02.api.dto.AttendanceScheduleView;
import java.time.LocalDate;
import java.util.*;

/** Bản giả chung duy nhất của hàm lịch đến 004; chỉ nạp trong test, B review. */
public class FakeContractSchedules implements ContractReadApi {
    private final Map<Long, List<AttendanceScheduleView>> schedules = new HashMap<>();
    private boolean available = true;

    public FakeContractSchedules schedule(long resident, AttendanceScheduleView value) {
        schedules.computeIfAbsent(resident, id -> new ArrayList<>()).add(value);
        return this;
    }

    public FakeContractSchedules available(boolean value) {
        available = value;
        return this;
    }

    public FakeContractSchedules clear() {
        schedules.clear();
        available = true;
        return this;
    }

    public List<AttendanceScheduleView> attendanceOn(long residentId, LocalDate date) {
        if (!available) throw new IllegalStateException("Chưa có nguồn lịch đến 004");
        return schedules.getOrDefault(residentId, List.of()).stream()
                .filter(s -> s.date().equals(date))
                .toList();
    }
}
