package com.eldercare.backend.support.fake.roombedallocation;

import com.eldercare.backend.roombedallocation.api.dto.DayCareSession;
import com.eldercare.backend.roombedallocation.api.dto.SessionDate;
import com.eldercare.backend.roombedallocation.api.spi.DayCareArrivalCounter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Bản giả lịch đến của 004 (FR-040): trả số test đặt, không tự sinh lịch. */
public class FakeDayCareArrivals implements DayCareArrivalCounter {
    private final Map<SessionDate, Integer> counts = new HashMap<>();

    public FakeDayCareArrivals count(SessionDate session, int count) {
        counts.put(session, count);
        return this;
    }

    @Override
    public Map<SessionDate, Integer> countFrom(LocalDate fromDate, List<DayCareSession> sessions) {
        Set<Long> sessionIds = sessions.stream().map(DayCareSession::id).collect(Collectors.toSet());
        return counts.entrySet().stream()
                .filter(e -> !e.getKey().date().isBefore(fromDate)
                        && sessionIds.contains(e.getKey().sessionId()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public boolean hasFutureArrivals(LocalDate fromDate) {
        return counts.entrySet().stream().anyMatch(e -> !e.getKey().date().isBefore(fromDate) && e.getValue() > 0);
    }

    public void reset() {
        counts.clear();
    }
}
