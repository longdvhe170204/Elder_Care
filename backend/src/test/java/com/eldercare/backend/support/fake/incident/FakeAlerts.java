package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.incident.api.AlertApi;
import com.eldercare.backend.incident.api.dto.AlertView;
import java.time.Instant;
import java.util.*;

/** Nguồn cảnh báo chỉ đọc do test cấu hình. */
public class FakeAlerts implements AlertApi {
    private final Map<Long, AlertView> rows = new LinkedHashMap<>();

    public void put(AlertView row) {
        rows.put(row.id(), row);
    }

    @Override
    public Optional<AlertView> view(long id) {
        return Optional.ofNullable(rows.get(id));
    }

    @Override
    public List<AlertView> byResident(long residentId, Instant from, Instant to) {
        return rows.values().stream()
                .filter(r -> Objects.equals(r.residentId(), residentId))
                .filter(r -> !r.firstAt().isBefore(from) && r.firstAt().isBefore(to))
                .toList();
    }

    @Override
    public boolean hasOpenAlert(long residentId) {
        return rows.values().stream()
                .anyMatch(r -> Objects.equals(r.residentId(), residentId)
                        && !Set.of("CLOSED", "CONVERTED_TO_INCIDENT").contains(r.status()));
    }
}
