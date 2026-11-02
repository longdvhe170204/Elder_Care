package com.eldercare.backend.support.fake.health;

import com.eldercare.backend.health.api.events.MeasurementScheduleChanged;
import com.eldercare.backend.health.api.events.MissingPersonalThresholdReminder;
import com.eldercare.backend.health.api.events.VitalSignConfirmed;
import com.eldercare.backend.health.api.events.VitalSignCorrected;
import org.springframework.context.ApplicationEventPublisher;

/** Phát sự kiện do test đặt cho consumer của 007; không ghi số đo hoặc lịch. */
public class FakeHealthEvents {
    private final ApplicationEventPublisher publisher;

    public FakeHealthEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void confirmed(VitalSignConfirmed event) {
        publisher.publishEvent(event);
    }

    public void corrected(VitalSignCorrected event) {
        publisher.publishEvent(event);
    }

    public void scheduleChanged(MeasurementScheduleChanged event) {
        publisher.publishEvent(event);
    }

    public void missingThreshold(MissingPersonalThresholdReminder event) {
        publisher.publishEvent(event);
    }
}
