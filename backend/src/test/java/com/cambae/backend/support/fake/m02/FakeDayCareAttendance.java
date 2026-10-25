package com.cambae.backend.support.fake.m02;

import com.cambae.backend.m02.api.dto.Attendance;
import com.cambae.backend.m02.api.spi.DayCareAttendancePort;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Bản giả {@link DayCareAttendancePort} (005 cài): mặc định buổi chưa xác định (rỗng). */
public class FakeDayCareAttendance implements DayCareAttendancePort {

    private final Map<String, Attendance> attendance = new HashMap<>();

    private static String key(long residentId, LocalDate date, long sessionId) {
        return residentId + ":" + date + ":" + sessionId;
    }

    public FakeDayCareAttendance attendance(long residentId, LocalDate date, long sessionId, Attendance value) {
        attendance.put(key(residentId, date, sessionId), value);
        return this;
    }

    public FakeDayCareAttendance reset() {
        attendance.clear();
        return this;
    }

    @Override
    public Optional<Attendance> status(long residentId, LocalDate date, long sessionId) {
        return Optional.ofNullable(attendance.get(key(residentId, date, sessionId)));
    }
}
