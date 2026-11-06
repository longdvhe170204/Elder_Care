package com.eldercare.backend.support.fake.care;

import com.eldercare.backend.care.api.spi.TripStaffSchedulePort;
import java.time.Instant;
import java.util.*;

/** Dữ liệu ca đặt rõ theo từng nhân viên; không suy ca từ tầng của chuyến. */
public class FakeTripStaffSchedule implements TripStaffSchedulePort {
    private final Map<Long, List<Instant[]>> intervals = new HashMap<>();

    public void shift(long staff, Instant from, Instant to) {
        intervals.computeIfAbsent(staff, k -> new ArrayList<>()).add(new Instant[] {from, to});
    }

    @Override
    public boolean hasOverlappingPublishedShift(long staff, Instant from, Instant to) {
        return intervals.getOrDefault(staff, List.of()).stream().anyMatch(i -> i[0].isBefore(to) && i[1].isAfter(from));
    }
}
