package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.api.events.AlertRequested;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.incident.internal.alert.service.DietAlertRequestListener;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 011 FR-021, FR-065: xung đột dị ứng thực đơn → cảnh báo Trung bình báo Bác sĩ; phát lại không gộp thêm. */
class DietAllergyAlertRequestedTest {
    private final AlertService alerts = mock(AlertService.class);
    private final ReminderMarkers markers = mock(ReminderMarkers.class);
    private final DietAlertRequestListener listener = new DietAlertRequestListener(alerts, markers);
    private final Instant at = Instant.parse("2026-11-07T01:00:00Z");

    private AlertRequested event(String type, String level, String key) {
        return new AlertRequested(12, type, level, "MENU_DISH", 77, Optional.of("2026-11-08:3"), at, key);
    }

    @Test
    void dietAllergyConflictCreatesMediumAlertBySourceDish() {
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true);

        listener.onAlertRequested(event("DIET_ALLERGY_CONFLICT", "MEDIUM", "M08.DIET_ALLERGY_CONFLICT:1:x"));

        verify(alerts)
                .requestAlert(
                        argThat(c -> c.residentId() == 12
                                && c.type().equals("DIET_ALLERGY_CONFLICT")
                                && c.typeLock().equals("DIET_ALLERGY_CONFLICT:77")
                                && c.level() == IncidentLevel.MEDIUM
                                && c.sourceType().equals("MENU_DISH")
                                && c.sourceId() == 77
                                && c.occurredAt().equals(at)),
                        any());
    }

    @Test
    void doctorIsNotifiedWhenAlertIsCreated() {
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true);
        var captured = new java.util.concurrent.atomic.AtomicReference<java.util.List<?>>();
        when(alerts.requestAlert(any(), any())).thenAnswer(inv -> {
            java.util.function.Function<
                            AlertRecord, java.util.List<com.eldercare.backend.common.notification.NotificationRequest>>
                    f = inv.getArgument(1);
            var alert = mock(AlertRecord.class);
            when(alert.id()).thenReturn(5L);
            when(alert.residentId()).thenReturn(12L);
            captured.set(f.apply(alert));
            return Optional.empty();
        });

        listener.onAlertRequested(event("DIET_ALLERGY_CONFLICT", "MEDIUM", "k"));

        var notices = (java.util.List<com.eldercare.backend.common.notification.NotificationRequest>) captured.get();
        assertThat(notices).hasSize(1);
        assertThat(notices.get(0).recipientGroups()).anyMatch(g -> g instanceof RecipientGroup.OnDutyDoctor);
    }

    @Test
    void otherTypesAreIgnored() {
        listener.onAlertRequested(event("ALLERGEN_PORTION_SERVED", "MEDIUM", "k"));
        verify(alerts, never()).requestAlert(any(), any());
        verify(alerts, never()).requestAlert(any());
    }

    @Test
    void replayedEventDoesNotMergeAgain() {
        when(markers.mark(anyString(), anyString(), anyLong(), eq("k"), any())).thenReturn(true, false);
        listener.onAlertRequested(event("DIET_ALLERGY_CONFLICT", "MEDIUM", "k"));
        listener.onAlertRequested(event("DIET_ALLERGY_CONFLICT", "MEDIUM", "k"));
        verify(alerts, org.mockito.Mockito.times(1)).requestAlert(any(), any());
    }
}
