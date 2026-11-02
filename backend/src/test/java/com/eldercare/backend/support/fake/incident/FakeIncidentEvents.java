package com.eldercare.backend.support.fake.incident;

import com.eldercare.backend.incident.api.events.IncidentRecorded;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;

/** Phát sự kiện M05 trong test của module khác mà không cần cài luồng ghi sự cố thật. */
public class FakeIncidentEvents {

    private final ApplicationEventPublisher publisher;
    private final List<Object> published = new ArrayList<>();

    public FakeIncidentEvents(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public IncidentRecorded incidentRecorded(
            long incidentId, long residentId, String type, String level, boolean needsReassessment) {
        var event = new IncidentRecorded(incidentId, residentId, type, level, needsReassessment);
        return publish(event);
    }

    /** Dùng chung cho mọi sự kiện công khai của M05; không tạo thêm fake theo từng consumer. */
    public <T> T publish(T event) {
        published.add(event);
        publisher.publishEvent(event);
        return event;
    }

    public List<Object> published() {
        return List.copyOf(published);
    }
}
