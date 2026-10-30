package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.incident.api.events.IncidentRecorded;
import org.springframework.context.ApplicationEventPublisher;

/** Phát sự kiện M05 trong test của module khác mà không cần cài luồng ghi sự cố thật. */
public class FakeIncidentEvents {

    private final ApplicationEventPublisher publisher;

    public FakeIncidentEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public IncidentRecorded incidentRecorded(
            long incidentId, long residentId, String type, String level, boolean needsReassessment) {
        var event = new IncidentRecorded(incidentId, residentId, type, level, needsReassessment);
        publisher.publishEvent(event);
        return event;
    }
}
