package com.eldercare.backend.health.internal.schedule.service;

import com.eldercare.backend.health.api.MeasurementScheduleApi;
import com.eldercare.backend.health.api.dto.MeasurementScheduleView;
import com.eldercare.backend.health.internal.schedule.domain.MeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.MeasurementTimeline;
import com.eldercare.backend.health.internal.schedule.persistence.MeasurementScheduleStore;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bản thật đọc lịch đo cho module khác: lịch là căn cứ tại một thời điểm và chuỗi mốc đo để hiển thị (Q-306). */
@Service
public class DefaultMeasurementScheduleApi implements MeasurementScheduleApi {
    private final MeasurementScheduleStore store;

    public DefaultMeasurementScheduleApi(MeasurementScheduleStore store) {
        this.store = store;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MeasurementScheduleView> effectiveAt(long residentId, Instant at) {
        return store.byResident(residentId).stream()
                .filter(s -> s.basisAt(at))
                .map(DefaultMeasurementScheduleApi::view)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Instant> occurrences(long scheduleId, Instant from, Instant to) {
        return store.find(scheduleId).map(s -> occurrencesOf(s, from, to)).orElse(List.of());
    }

    /** Mốc đo khi lịch còn là căn cứ: dừng ở thời điểm ngừng nếu có. */
    static List<Instant> occurrencesOf(MeasurementSchedule schedule, Instant from, Instant to) {
        var content = schedule.content();
        var end = content.endAt();
        if (schedule.stoppedAt() != null && (end == null || schedule.stoppedAt().isBefore(end)))
            end = schedule.stoppedAt();
        return MeasurementTimeline.occurrences(content.frequency(), content.startAt(), end, from, to);
    }

    private static MeasurementScheduleView view(MeasurementSchedule schedule) {
        var content = schedule.content();
        var frequency = content.frequency();
        var parameters = new HashMap<String, Object>();
        parameters.put("times", frequency.times().stream().map(Object::toString).toList());
        if (frequency.everyHours() != null) parameters.put("everyHours", frequency.everyHours());
        if (frequency.anchorAt() != null) parameters.put("anchorAt", frequency.anchorAt());
        parameters.put("weekdays", List.copyOf(frequency.weekdays()));
        return new MeasurementScheduleView(
                schedule.id(),
                schedule.residentId(),
                content.indicatorIds(),
                frequency.type(),
                parameters,
                content.startAt(),
                content.endAt(),
                content.priority(),
                content.notes());
    }
}
