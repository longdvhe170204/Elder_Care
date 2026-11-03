package com.eldercare.backend.support.fake.health;

import com.eldercare.backend.health.api.MeasurementScheduleApi;
import com.eldercare.backend.health.api.dto.MeasurementScheduleView;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Test đặt kết quả truy vấn; không cài thuật toán tính lịch hoặc tạo công việc. */
public class FakeMeasurementSchedules implements MeasurementScheduleApi {
    private record EffectiveQuery(long residentId, Instant at) {}

    private record OccurrenceQuery(long scheduleId, Instant from, Instant to) {}

    private final Map<EffectiveQuery, List<MeasurementScheduleView>> effective = new HashMap<>();
    private final Map<OccurrenceQuery, List<Instant>> occurrences = new HashMap<>();

    public void effective(long residentId, Instant at, List<MeasurementScheduleView> rows) {
        effective.put(new EffectiveQuery(residentId, at), List.copyOf(rows));
    }

    public void occurrences(long scheduleId, Instant from, Instant to, List<Instant> values) {
        occurrences.put(new OccurrenceQuery(scheduleId, from, to), List.copyOf(values));
    }

    @Override
    public List<MeasurementScheduleView> effectiveAt(long residentId, Instant at) {
        return effective.getOrDefault(new EffectiveQuery(residentId, at), List.of());
    }

    @Override
    public List<Instant> occurrences(long scheduleId, Instant from, Instant to) {
        return occurrences.getOrDefault(new OccurrenceQuery(scheduleId, from, to), List.of());
    }
}
