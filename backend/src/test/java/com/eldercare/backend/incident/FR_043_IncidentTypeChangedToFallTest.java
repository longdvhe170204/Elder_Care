package com.eldercare.backend.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.IncidentRecorded;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.domain.IncidentView;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentCorrectionEffects;
import com.eldercare.backend.incident.internal.incident.service.IncidentRecordService;
import com.eldercare.backend.incident.internal.infection.persistence.ContactListStore;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/** FR-043 (001 FR-024): đổi loại sang Ngã bằng đính chính phát lại {@code IncidentRecorded} để 001 báo đánh giá lại. */
class FR_043_IncidentTypeChangedToFallTest {
    private final IncidentRecordService records = mock(IncidentRecordService.class);
    private final IncidentStore incidents = mock(IncidentStore.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final IncidentCorrectionEffects effects = new IncidentCorrectionEffects(
            records,
            incidents,
            mock(FallMonitoringStore.class),
            mock(ContactListStore.class),
            mock(ContainmentStore.class),
            events,
            time);

    @Test
    void FR_043_typeChangedToFallEmitsIncidentRecordedWithCurrentLevel() {
        var incident = mock(IncidentView.class);
        when(incident.id()).thenReturn(41L);
        when(incident.residentId()).thenReturn(101L);
        when(incident.level()).thenReturn(IncidentLevel.LOW);
        when(time.now()).thenReturn(Instant.parse("2026-11-08T03:00:00Z"));

        effects.typeChanged(incident, "OTHER_INJURY", "FALL", 5L, 9L);

        verify(events).publishEvent(new IncidentRecorded(41L, 101L, "FALL", "LOW", true));
    }

    @Test
    void FR_043_otherTypeChangesDoNotEmit() {
        var incident = mock(IncidentView.class);
        when(incident.id()).thenReturn(41L);
        when(incident.residentId()).thenReturn(101L);
        when(incident.level()).thenReturn(IncidentLevel.LOW);
        when(time.now()).thenReturn(Instant.parse("2026-11-08T03:00:00Z"));

        effects.typeChanged(incident, "FALL", "OTHER_INJURY", 5L, 9L);

        verify(events, never()).publishEvent(any(IncidentRecorded.class));
    }
}
