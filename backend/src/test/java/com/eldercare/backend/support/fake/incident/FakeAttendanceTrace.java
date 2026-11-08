package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.care.api.dto.ActivityAttendanceTraceView;
import com.eldercare.backend.care.api.spi.AttendanceTracePort;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Các buổi cùng tham gia do test đặt theo người cần truy vết. */
public class FakeAttendanceTrace implements AttendanceTracePort {
    private final Map<Long, List<ActivityAttendanceTraceView>> sessions = new HashMap<>();

    public FakeAttendanceTrace sessions(long residentId, List<ActivityAttendanceTraceView> rows) {
        sessions.put(residentId, List.copyOf(rows));
        return this;
    }

    @Override
    public List<ActivityAttendanceTraceView> coAttendance(long residentId, Instant from, Instant to) {
        return sessions.getOrDefault(residentId, List.of()).stream()
                .filter(session -> session.from().isBefore(to) && session.to().isAfter(from))
                .toList();
    }
}
