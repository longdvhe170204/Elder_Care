package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.AlertRequested;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.service.AlertRequestListener;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Cảnh báo 005 đi qua consumer 007, giữ nguồn, mốc nghiệp vụ và chống phát lại. */
class BR_M05_02_CareAlertRequestedTest {
    private final AlertService alerts = mock(AlertService.class);
    private final ReminderMarkers markers = mock(ReminderMarkers.class);
    private final AlertRequestListener listener = new AlertRequestListener(alerts, markers);

    @Test
    void BR_M05_02_careLowAlertsUseResidentTypeAndSourceFromPayload() {
        var at = Instant.parse("2026-11-03T10:00:00Z");
        var context =
                new CareEventContext("M04.CARE_RESULT", 54, "WATER_CHECK:2026-11-03", at, Optional.of(8L), List.of());
        var event = new AlertRequested(
                "LOW_WATER_INTAKE", NotificationLevel.LOW, 12, "M04", SourceType.CARE_PLAN_ITEM, 31, context);
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true);

        listener.onCareAlertRequested(event);

        verify(alerts)
                .requestAlert(argThat(command -> command.residentId() == 12
                        && command.floorAreaId() == 8
                        && command.type().equals("LOW_WATER_INTAKE")
                        && command.typeLock().equals("LOW_WATER_INTAKE")
                        && command.level() == IncidentLevel.LOW
                        && command.occurredAt().equals(at)
                        && command.sourceType().equals("M04.CARE_RESULT")
                        && command.sourceId() == 54));
        assertThat(event.eventKey()).contains("AlertRequested:54:");
    }

    @Test
    void BR_M05_02_replayedCareEventDoesNotIncrementAlertAgain() {
        var at = Instant.parse("2026-11-03T10:00:00Z");
        var event = new AlertRequested(
                "PROLONGED_NEGATIVE_MOOD",
                NotificationLevel.LOW,
                12,
                "M04",
                SourceType.CARE_PLAN_ITEM,
                31,
                new CareEventContext(
                        "M04.CARE_RESULT", 54, "NEGATIVE_MOOD:2026-11-03", at, Optional.empty(), List.of()));
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true, false);

        listener.onCareAlertRequested(event);
        listener.onCareAlertRequested(event);

        verify(alerts, times(1)).requestAlert(any());
    }
}
