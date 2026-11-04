package com.eldercare.backend.health.internal.schedule.service;

import com.eldercare.backend.health.internal.scheduling.service.M06TimedTask;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Việc theo phút của M06: lịch đo Chờ hiệu lực → Hiệu lực, Hiệu lực → Hết hạn (bảng FR-006). */
@Component
public class MeasurementScheduleTask implements M06TimedTask {
    private final MeasurementScheduleService schedules;

    public MeasurementScheduleTask(MeasurementScheduleService schedules) {
        this.schedules = schedules;
    }

    @Override
    public String name() {
        return "MEASUREMENT_SCHEDULE_STATUS";
    }

    @Override
    public Cadence cadence() {
        return Cadence.MINUTE;
    }

    @Override
    public int run(Instant scheduledAt, Instant actualAt) {
        return schedules.transitionDue(actualAt);
    }
}
