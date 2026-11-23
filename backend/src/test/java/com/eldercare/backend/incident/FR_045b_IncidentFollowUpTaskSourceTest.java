package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentFollowUpTaskSource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** FR-045b: việc tiếp theo chỉ lập được cho sự cố đang mở của chính người cao tuổi. */
class FR_045b_IncidentFollowUpTaskSourceTest {
    private static final Instant AT = Instant.parse("2026-11-23T10:00:00Z");

    private final IncidentStore incidents = mock(IncidentStore.class);
    private final IncidentFollowUpTaskSource source =
            new IncidentFollowUpTaskSource(incidents, mock(IncidentCatalogService.class));

    private static IncidentView view(String status) {
        return new IncidentView(
                41,
                101L,
                1,
                1,
                null,
                null,
                AT,
                7L,
                null,
                null,
                "Ngã",
                null,
                IncidentLevel.CRITICAL,
                null,
                null,
                false,
                900L,
                status,
                IncidentLevel.CRITICAL,
                true,
                0,
                900L,
                AT,
                false,
                null);
    }

    @Test
    void FR_045b_openIncidentOfSameResidentIsValid() {
        when(incidents.view(41)).thenReturn(Optional.of(view("MONITORING")));
        var result = source.validate(SourceType.INCIDENT_FOLLOW_UP, 41, 101, AT);
        assertThat(result.valid()).isTrue();
        assertThat(result.residentMatches()).isTrue();
        assertThat(result.timeMatches()).isTrue();
    }

    @Test
    void FR_045b_closedIncidentOrOtherResidentIsInvalid() {
        when(incidents.view(41)).thenReturn(Optional.of(view("CLOSED")));
        assertThat(source.validate(SourceType.INCIDENT_FOLLOW_UP, 41, 101, AT).valid())
                .isFalse();
        when(incidents.view(41)).thenReturn(Optional.of(view("MONITORING")));
        assertThat(source.validate(SourceType.INCIDENT_FOLLOW_UP, 41, 202, AT).residentMatches())
                .isFalse();
        assertThat(source.validate(SourceType.INCIDENT_FOLLOW_UP, 99, 101, AT).valid())
                .isFalse();
    }

    @Test
    void FR_045b_listsOpenIncidentsOfResidentForShiftPlanning() {
        when(incidents.search(any(), eq(101L), any(), anyBoolean(), any(), any()))
                .thenReturn(List.of(view("MONITORING")));
        assertThat(source.sources(101, AT, AT.plusSeconds(60))).singleElement().satisfies(row -> {
            assertThat(row.type()).isEqualTo(SourceType.INCIDENT_FOLLOW_UP);
            assertThat(row.id()).isEqualTo(41);
            assertThat(row.description()).contains("SC-41");
        });
    }
}
