package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.CallReminderStopRequested;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.domain.IncidentState;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentService;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class BR_M05_22_IncidentLevelChangeTest {
    private final IncidentStore incidents = mock(IncidentStore.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final IncidentService service = new IncidentService(
            incidents, permissions, audit, events, mock(BusinessTime.class), mock(IncidentProgressionStore.class));

    @BeforeEach
    void allowedDoctor() {
        when(user.roles()).thenReturn(Set.of(Role.DOCTOR));
        when(permissions.hasPermission(
                        org.mockito.ArgumentMatchers.eq(user),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
    }

    @Test
    void BR_M05_22_loweringFromCriticalStopsPendingCallReminders() {
        var state = new IncidentState(41, 101L, 7L, "IN_PROGRESS", IncidentLevel.CRITICAL, true, 3);
        when(incidents.lock(41)).thenReturn(Optional.of(state));

        service.changeLevel(null, 41, IncidentLevel.MEDIUM, new Reason("Đã ổn định"), user);

        verify(incidents).changeLevel(41, IncidentLevel.MEDIUM, true, 3);
        verify(events).publishEvent(new CallReminderStopRequested("M05.INCIDENT", 41));
        verify(audit)
                .write(org.mockito.ArgumentMatchers.argThat(
                        entry -> entry.entityType().equals("M05.INCIDENT")
                                && entry.reason().equals("Đã ổn định")
                                && entry.before().get("level").equals("CRITICAL")
                                && entry.after().get("level").equals("MEDIUM")));
    }

    @Test
    void FR_044_onlyDoctorMayLowerCriticalIncident() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        when(incidents.lock(41))
                .thenReturn(
                        Optional.of(new IncidentState(41, 101L, 7L, "IN_PROGRESS", IncidentLevel.CRITICAL, true, 3)));

        assertThatThrownBy(() -> service.changeLevel(null, 41, IncidentLevel.MEDIUM, new Reason("Đã ổn định"), user))
                .isInstanceOf(PermissionDeniedException.class);

        verify(incidents, never())
                .changeLevel(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyBoolean(),
                        org.mockito.ArgumentMatchers.anyInt());
        verify(events, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void FR_043_raisingFromLowReEmitsIncidentRecordedForReassessmentOnce() {
        when(incidents.lock(41))
                .thenReturn(Optional.of(new IncidentState(41, 101L, 7L, "NEW", IncidentLevel.LOW, false, 3)));
        when(incidents.currentTypeCode(41)).thenReturn("OTHER_INJURY");

        service.changeLevel(null, 41, IncidentLevel.MEDIUM, new Reason("Nặng hơn"), user);

        verify(events)
                .publishEvent(new com.eldercare.backend.incident.api.events.IncidentRecorded(
                        41, 101L, "OTHER_INJURY", "MEDIUM", true));
    }

    @Test
    void FR_043_raisingBelongingsIncidentOrFromMediumDoesNotReEmit() {
        when(incidents.lock(41))
                .thenReturn(Optional.of(new IncidentState(41, 101L, 7L, "NEW", IncidentLevel.LOW, false, 3)));
        when(incidents.currentTypeCode(41)).thenReturn("BELONGINGS_LOST");
        service.changeLevel(null, 41, IncidentLevel.MEDIUM, new Reason("Nặng hơn"), user);
        when(incidents.lock(42))
                .thenReturn(Optional.of(new IncidentState(42, 101L, 7L, "NEW", IncidentLevel.MEDIUM, false, 3)));
        when(incidents.currentTypeCode(42)).thenReturn("OTHER_INJURY");
        service.changeLevel(null, 42, IncidentLevel.CRITICAL, new Reason("Nặng hơn"), user);

        verify(events, never())
                .publishEvent(org.mockito.ArgumentMatchers.any(
                        com.eldercare.backend.incident.api.events.IncidentRecorded.class));
    }

    @Test
    void FR_044_raisingToCriticalKeepsEmergencyHistoryWithoutStopEvent() {
        when(incidents.lock(41))
                .thenReturn(
                        Optional.of(new IncidentState(41, 101L, 7L, "IN_PROGRESS", IncidentLevel.MEDIUM, false, 3)));

        service.changeLevel(null, 41, IncidentLevel.CRITICAL, new Reason("Diễn biến nặng"), user);

        verify(incidents).changeLevel(41, IncidentLevel.CRITICAL, true, 3);
        verify(events, never()).publishEvent(org.mockito.ArgumentMatchers.any(CallReminderStopRequested.class));
    }
}
