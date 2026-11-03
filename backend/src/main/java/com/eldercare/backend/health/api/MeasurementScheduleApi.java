package com.eldercare.backend.health.api;

import com.eldercare.backend.health.api.dto.MeasurementScheduleView;
import java.time.Instant;
import java.util.List;

/** Đọc lịch đo; mốc đo chỉ để hiển thị, không tạo công việc (Q-306). */
public interface MeasurementScheduleApi {
    List<MeasurementScheduleView> effectiveAt(long residentId, Instant at);

    List<Instant> occurrences(long scheduleId, Instant from, Instant to);
}
