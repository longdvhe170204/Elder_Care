package com.cambae.backend.support.fake.m03;

import com.cambae.backend.m03.api.dto.DayCareSession;
import com.cambae.backend.m03.api.dto.SessionDate;
import com.cambae.backend.m03.api.spi.DayCareAttendanceCounter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Bản giả số lịch đến do 004 cung cấp; test đặt số đếm theo khung buổi cần kiểm (003 T002). */
public class FakeDayCareAttendances implements DayCareAttendanceCounter {

    private final Map<SessionDate, Integer> counts = new HashMap<>();

    public FakeDayCareAttendances count(SessionDate session, int count) {
        counts.put(session, count);
        return this;
    }

    public void reset() {
        counts.clear();
    }

    @Override
    public Map<SessionDate, Integer> countFrom(LocalDate fromDate, List<DayCareSession> sessions) {
        Set<Long> sessionIds = sessions.stream().map(DayCareSession::id).collect(Collectors.toSet());
        Map<SessionDate, Integer> result = new HashMap<>();
        counts.forEach((session, count) -> {
            if (!session.date().isBefore(fromDate) && sessionIds.contains(session.sessionId())) {
                result.put(session, count);
            }
        });
        return Map.copyOf(result);
    }

    @Override
    public boolean hasFutureSchedule(LocalDate fromDate) {
        return counts.entrySet().stream()
                .anyMatch(entry -> !entry.getKey().date().isBefore(fromDate) && entry.getValue() > 0);
    }
}
