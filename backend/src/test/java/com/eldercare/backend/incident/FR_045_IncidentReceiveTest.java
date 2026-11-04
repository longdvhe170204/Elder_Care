package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.domain.IncidentState;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/** Lệnh "Tiếp nhận xử lý" sự cố (FR-044, FR-045, FR-036): Mới → Đang xử lý. */
class FR_045_IncidentReceiveTest {
    private static final Instant NOW = Instant.parse("2026-11-04T01:00:00Z");

    private final IncidentStore incidents = mock(IncidentStore.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final IncidentService service = new IncidentService(incidents, permissions, audit, events, time);

    @BeforeEach
    void allowedNurse() {
        when(time.now()).thenReturn(NOW);
        when(user.accountId()).thenReturn(900L);
        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        when(permissions.hasPermission(
                        org.mockito.ArgumentMatchers.eq(user),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
    }

    private static IncidentState state(String status) {
        return new IncidentState(41, 101L, 7L, status, IncidentLevel.MEDIUM, false, 3);
    }

    @Test
    void FR_045_receiveMovesNewIncidentToInProgressAndStopsEscalation() {
        when(incidents.lock(41)).thenReturn(Optional.of(state("NEW")));

        service.receive(41, user);

        verify(incidents).receive(41, 900L, NOW, 3);
        verify(audit)
                .write(org.mockito.ArgumentMatchers.argThat(
                        entry -> entry.entityType().equals("M05.INCIDENT")
                                && entry.before().get("status").equals("NEW")
                                && entry.after().get("status").equals("IN_PROGRESS")));
    }

    @Test
    void FR_045_receiveAlreadyReceivedIncidentIsConflict() {
        when(incidents.lock(41)).thenReturn(Optional.of(state("IN_PROGRESS")));

        assertThatThrownBy(() -> service.receive(41, user)).isInstanceOfSatisfying(BusinessException.class, e -> {
            org.assertj.core.api.Assertions.assertThat(e.errorCode()).isEqualTo("ALREADY_RECEIVED");
        });

        verify(incidents, never())
                .receive(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void FR_036_managerAndCaregiverMayNotReceive() {
        when(incidents.lock(41)).thenReturn(Optional.of(state("NEW")));
        for (var role : new Role[] {Role.NURSING_HOME_MANAGER, Role.CAREGIVER}) {
            when(user.roles()).thenReturn(Set.of(role));
            assertThatThrownBy(() -> service.receive(41, user)).isInstanceOf(PermissionDeniedException.class);
        }
        verify(incidents, never())
                .receive(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void FR_036_outsideScopeLooksLikeNotFound() {
        when(incidents.lock(41)).thenReturn(Optional.of(state("NEW")));
        when(permissions.hasPermission(
                        org.mockito.ArgumentMatchers.eq(user),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(PermissionResult.denied(FailedLayer.SCOPE));

        assertThatThrownBy(() -> service.receive(41, user)).isInstanceOf(NotFoundException.class);
    }
}
