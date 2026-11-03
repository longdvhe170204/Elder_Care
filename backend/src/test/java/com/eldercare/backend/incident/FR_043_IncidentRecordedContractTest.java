package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.api.events.IncidentRecorded;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FR_043_IncidentRecordedContractTest {

    @Test
    void FR_043_incidentRecordedCarriesTheApprovedCrossModulePayload() {
        var event = new IncidentRecorded(101L, 202L, "FALL", "MEDIUM", true);

        assertThat(event.incidentId()).isEqualTo(101L);
        assertThat(event.residentId()).isEqualTo(202L);
        assertThat(event.type()).isEqualTo("FALL");
        assertThat(event.level()).isEqualTo("MEDIUM");
        assertThat(event.needsReassessment()).isTrue();
        assertThat(Arrays.stream(IncidentRecorded.class.getRecordComponents()).map(component -> component.getName()))
                .containsExactly("incidentId", "residentId", "type", "level", "needsReassessment");
    }
}
