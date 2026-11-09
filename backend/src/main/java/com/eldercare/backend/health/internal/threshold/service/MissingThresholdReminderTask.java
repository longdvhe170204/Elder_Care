package com.eldercare.backend.health.internal.threshold.service;

import com.eldercare.backend.health.internal.scheduling.service.M06TimedTask;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Việc hằng ngày của M06: nhắc thiếu ngưỡng cá nhân (FR-025, BR-M06-02). */
@Component
public class MissingThresholdReminderTask implements M06TimedTask {
    private final MissingThresholdReminders reminders;

    public MissingThresholdReminderTask(MissingThresholdReminders reminders) {
        this.reminders = reminders;
    }

    @Override
    public String name() {
        return "MISSING_PERSONAL_THRESHOLD";
    }

    @Override
    public Cadence cadence() {
        return Cadence.DAILY;
    }

    @Override
    public int run(Instant scheduledAt, Instant actualAt) {
        return reminders.remind(actualAt);
    }
}
