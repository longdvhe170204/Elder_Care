package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.InfectionSuspicionChanged;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionGroup;
import com.eldercare.backend.incident.internal.alert.domain.ProgressionTypeView;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.incident.domain.IncidentState;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentHandlingService;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/** Chuyển theo dõi, mở lại, đóng, tham gia xử lý sự cố (bảng FR-045; FR-041, FR-066, BR-M05-09, Q-71). */
class FR_045_IncidentHandlingTest {
    private static final Instant NOW = Instant.parse("2026-11-04T04:00:00Z");
    private static final long EVALUATION = 1L;
    private static final long ACTION = 2L;

    private final IncidentStore incidents = mock(IncidentStore.class);
    private final IncidentProgressionStore progressions = mock(IncidentProgressionStore.class);
    private final IncidentCatalogService catalog = mock(IncidentCatalogService.class);
    private final ContainmentStore containments = mock(ContainmentStore.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final IncidentHandlingService service = new IncidentHandlingService(
            incidents, progressions, catalog, containments, permissions, audit, events, time);

    @BeforeEach
    void defaults() {
        when(time.now()).thenReturn(NOW);
        when(user.accountId()).thenReturn(900L);
        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        when(permissions.hasPermission(eq(user), any(), any()))
                .thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(catalog.progressionTypes())
                .thenReturn(List.of(
                        new ProgressionTypeView(
                                EVALUATION, "ASSESSED", "Đánh giá", ProgressionGroup.EVALUATION, false, 0),
                        new ProgressionTypeView(
                                ACTION, "ACTION_TAKEN", "Hành động", ProgressionGroup.ACTION, false, 0)));
        when(incidents.currentTypeCode(41)).thenReturn("OTHER");
        when(containments.activeOnlyFor(41)).thenReturn(false);
    }

    private void incident(String status, IncidentLevel level) {
        when(incidents.lock(41)).thenReturn(Optional.of(new IncidentState(41, 101L, 7L, status, level, false, 3)));
    }

    @Test
    void FR_045_moveToMonitoringNeedsEvaluationAndActionProgression() {
        incident("IN_PROGRESS", IncidentLevel.MEDIUM);
        when(progressions.typeIds(41)).thenReturn(List.of(ACTION));

        assertThatThrownBy(() -> service.moveToMonitoring(41, user)).isInstanceOf(BusinessException.class);
        verify(incidents, never()).changeStatus(anyLong(), any(), any(), anyInt());

        when(progressions.typeIds(41)).thenReturn(List.of(ACTION, EVALUATION));
        service.moveToMonitoring(41, user);
        verify(incidents).changeStatus(41, "IN_PROGRESS", "MONITORING", 3);
    }

    @Test
    void FR_045_reopenOnlyFromMonitoring() {
        incident("IN_PROGRESS", IncidentLevel.MEDIUM);
        assertThatThrownBy(() -> service.reopen(null, 41, new Reason("Diễn biến xấu"), user))
                .isInstanceOf(BusinessException.class);

        incident("MONITORING", IncidentLevel.MEDIUM);
        service.reopen(null, 41, new Reason("Diễn biến xấu"), user);
        verify(incidents).changeStatus(41, "MONITORING", "IN_PROGRESS", 3);
    }

    @Test
    void BR_M05_09_floorManagerClosesOnlyLowIncident() {
        when(user.roles()).thenReturn(Set.of(Role.FLOOR_MANAGER));
        incident("IN_PROGRESS", IncidentLevel.MEDIUM);
        assertThatThrownBy(() -> service.close(41, "Đã ổn", null, user)).isInstanceOf(PermissionDeniedException.class);
        verify(incidents, never()).close(anyLong(), any(), any(), anyLong(), any(), anyInt());

        incident("MONITORING", IncidentLevel.LOW);
        service.close(41, "Đã ổn", null, user);
        verify(incidents).close(41, "Đã ổn", null, 900L, NOW, 3);
    }

    @Test
    void BR_M05_09_resolutionRequiredAndOnlyFromHandlingStates() {
        incident("IN_PROGRESS", IncidentLevel.MEDIUM);
        assertThatThrownBy(() -> service.close(41, " ", null, user)).isInstanceOf(BusinessException.class);

        incident("NEW", IncidentLevel.MEDIUM);
        assertThatThrownBy(() -> service.close(41, "Đã ổn", null, user)).isInstanceOf(BusinessException.class);
        verify(incidents, never()).close(anyLong(), any(), any(), anyLong(), any(), anyInt());
    }

    @Test
    void FR_066_infectionNeedsOutcomeAndNoContainmentOnlyForIt() {
        incident("IN_PROGRESS", IncidentLevel.MEDIUM);
        when(incidents.currentTypeCode(41)).thenReturn("SUSPECTED_INFECTION");
        assertThatThrownBy(() -> service.close(41, "Đã cách ly", null, user)).isInstanceOf(BusinessException.class);

        when(containments.activeOnlyFor(41)).thenReturn(true);
        assertThatThrownBy(() -> service.close(41, "Đã cách ly", "RULED_OUT", user))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("ISOLATION_ONLY_FOR_THIS_INCIDENT"));
        verify(incidents, never()).close(anyLong(), any(), any(), anyLong(), any(), anyInt());

        when(containments.activeOnlyFor(41)).thenReturn(false);
        service.close(41, "Đã cách ly", "RULED_OUT", user);
        verify(incidents).close(41, "Đã cách ly", "RULED_OUT", 900L, NOW, 3);
        verify(events).publishEvent(new InfectionSuspicionChanged(101L, 41L, false, NOW));
    }

    @Test
    void FR_041_joinOnlyOpenCriticalIncident() {
        incident("IN_PROGRESS", IncidentLevel.MEDIUM);
        assertThatThrownBy(() -> service.join(41, user)).isInstanceOf(BusinessException.class);
        verify(progressions, never()).join(anyLong(), anyLong(), any(), any());

        incident("IN_PROGRESS", IncidentLevel.CRITICAL);
        service.join(41, user);
        verify(progressions).join(41, 900L, NOW, 7L);
    }

    @Test
    void Q_71_managerMayNotHandle() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        incident("IN_PROGRESS", IncidentLevel.CRITICAL);
        when(progressions.typeIds(41)).thenReturn(List.of(ACTION, EVALUATION));

        assertThatThrownBy(() -> service.moveToMonitoring(41, user)).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> service.close(41, "Xong", null, user)).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> service.join(41, user)).isInstanceOf(PermissionDeniedException.class);
    }
}
