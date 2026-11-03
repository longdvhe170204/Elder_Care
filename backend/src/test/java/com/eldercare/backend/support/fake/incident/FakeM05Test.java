package com.eldercare.backend.support.fake.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.api.events.IncidentRecorded;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class FakeM05Test {

    @Test
    void FR_043_incidentEventsPublishesTheApprovedEventForConsumerTests() {
        List<Object> published = new ArrayList<>();
        FakeIncidentEvents events = new FakeIncidentEvents(published::add);

        var event = events.incidentRecorded(101L, 202L, "FALL", "MEDIUM", true);

        assertThat(published).containsExactly(event);
        assertThat(event).isEqualTo(new IncidentRecorded(101L, 202L, "FALL", "MEDIUM", true));
    }
}
