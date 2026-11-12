package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeView;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import com.eldercare.backend.incident.internal.incident.service.FamilyIncidentPartitioner;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** FR-082 (Q-352, Q-358, Q-72): sự cố cho người thân tách phần chung và phần sức khỏe. */
class FR_082_FamilyIncidentPartsTest {
    private static final Instant AT = Instant.parse("2030-03-04T03:00:00Z");

    private static IncidentView incident() {
        var incident = mock(IncidentView.class);
        when(incident.id()).thenReturn(41L);
        when(incident.occurredAt()).thenReturn(AT);
        when(incident.level()).thenReturn(IncidentLevel.MEDIUM);
        when(incident.status()).thenReturn("CLOSED");
        when(incident.description()).thenReturn("Ngã ở hành lang");
        when(incident.initialResponse()).thenReturn("Sơ cứu");
        return incident;
    }

    private static IncidentTypeView type(String code) {
        var type = mock(IncidentTypeView.class);
        when(type.code()).thenReturn(code);
        when(type.name()).thenReturn("Tên " + code);
        return type;
    }

    @Test
    void FR_082_generalPartAlwaysHealthPartOnlyWithConsentAndInfectionHidesTypeAndLevel() {
        var shared = FamilyIncidentPartitioner.partition(
                incident(), type("FALL"), List.of("Theo dõi"), Optional.empty(), true);
        assertThat(shared.general().typeName()).isEqualTo("Tên FALL");
        assertThat(shared.general().level()).isEqualTo("MEDIUM");
        assertThat(shared.general().processingStatus()).isEqualTo("RESOLVED");
        assertThat(shared.health()).get().satisfies(health -> {
            assertThat(health.description()).isEqualTo("Ngã ở hành lang");
            assertThat(health.initialResponse()).isEqualTo("Sơ cứu");
            assertThat(health.progressions()).containsExactly("Theo dõi");
        });

        var general = FamilyIncidentPartitioner.partition(
                incident(), type("FALL"), List.of("Theo dõi"), Optional.empty(), false);
        assertThat(general.general().occurredAt()).isEqualTo(AT);
        assertThat(general.health()).isEmpty();

        var infection = FamilyIncidentPartitioner.partition(
                incident(), type("SUSPECTED_INFECTION"), List.of(), Optional.empty(), false);
        assertThat(infection.general().typeName()).isNull();
        assertThat(infection.general().level()).isNull();
        assertThat(infection.general().occurredAt()).isEqualTo(AT);
    }
}
