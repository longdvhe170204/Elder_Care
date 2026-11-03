package com.eldercare.backend.health.internal.measurement.service;

import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import com.eldercare.backend.health.internal.scheduling.service.M06TimedTask;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** FR-017: việc theo phút xác nhận các bản nháp đã quá hạn đo lại. */
@Component
public class OverdueRemeasurementTask implements M06TimedTask {
    private final VitalSignRecordStore records;
    private final VitalSignMeasurementService measurements;

    public OverdueRemeasurementTask(VitalSignRecordStore records, VitalSignMeasurementService measurements) {
        this.records = records;
        this.measurements = measurements;
    }

    @Override
    public String name() {
        return "CONFIRM_OVERDUE_REMEASUREMENT";
    }

    @Override
    public Cadence cadence() {
        return Cadence.MINUTE;
    }

    @Override
    public int run(Instant scheduledAt, Instant actualAt) {
        int processed = 0;
        for (long id : records.overdueRemeasurementIds(actualAt)) {
            if (measurements.confirmOverdue(id, actualAt)) processed++;
        }
        return processed;
    }
}
