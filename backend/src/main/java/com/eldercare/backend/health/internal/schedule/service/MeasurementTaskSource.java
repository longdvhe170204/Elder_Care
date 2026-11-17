package com.eldercare.backend.health.internal.schedule.service;

import com.eldercare.backend.care.api.spi.TaskSourcePort;
import com.eldercare.backend.health.internal.schedule.persistence.MeasurementScheduleStore;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Nguồn công việc {@code MEASUREMENT} cho 005 (007 FR-004, T023; research S-05): căn cứ là mã lịch đo. Hợp lệ khi lịch còn
 * là căn cứ tại mốc dự kiến, đúng người cao tuổi và mốc thuộc chuỗi mốc đo của lịch; lịch mức Bắt buộc trả "theo chỉ định"
 * để 005 buộc công việc mức Bắt buộc (005 FR-023). Chỉ đọc, không tạo công việc (Q-306).
 */
@Component
public class MeasurementTaskSource implements TaskSourcePort {
    private final MeasurementScheduleStore store;

    public MeasurementTaskSource(MeasurementScheduleStore store) {
        this.store = store;
    }

    @Override
    public Set<SourceType> managedTypes() {
        return Set.of(SourceType.MEASUREMENT);
    }

    @Override
    public SourceValidation validate(SourceType type, long sourceId, long residentId, Instant plannedAt) {
        var found = store.find(sourceId);
        if (found.isEmpty()) return new SourceValidation(false, false, false, false);
        var schedule = found.get();
        boolean resident = schedule.residentId() == residentId;
        boolean time = plannedAt != null
                && schedule.basisAt(plannedAt)
                && DefaultMeasurementScheduleApi.occurrencesOf(schedule, plannedAt, plannedAt.plusSeconds(1))
                        .contains(plannedAt);
        return new SourceValidation(
                resident && time,
                resident,
                time,
                "MANDATORY".equals(schedule.content().priority()));
    }

    @Override
    public List<SourceView> sources(long residentId, Instant from, Instant to) {
        return store.byResident(residentId).stream()
                .flatMap(schedule -> DefaultMeasurementScheduleApi.occurrencesOf(schedule, from, to).stream()
                        .map(at -> new SourceView(SourceType.MEASUREMENT, schedule.id(), at, "Lịch đo theo chỉ định")))
                .toList();
    }
}
