package com.eldercare.backend.incident;

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
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.alert.domain.AlertReceiveState;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.AlertCommandService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Lệnh "Tiếp nhận" cảnh báo (bảng FR-029, FR-036, Q-71). */
class FR_029_AlertReceiveTest {
    private static final Instant NOW = Instant.parse("2026-11-04T03:00:00Z");

    private final AlertStore alerts = mock(AlertStore.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final AlertCommandService service = new AlertCommandService(alerts, permissions, audit, time);

    @BeforeEach
    void defaults() {
        when(time.now()).thenReturn(NOW);
        when(user.accountId()).thenReturn(900L);
        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        when(permissions.hasPermission(eq(user), any(), any()))
                .thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(alerts.lockForReceive(12)).thenReturn(Optional.of(state(AlertStatus.NEW, 0)));
    }

    private static AlertReceiveState state(AlertStatus status, int escalationLevel) {
        return new AlertReceiveState(12, 101L, 7L, status, escalationLevel, IncidentLevel.MEDIUM, null, null, 4);
    }

    @Test
    void Q_71_managerReceivesOnlyAtEscalationLevelTwo() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        when(permissions.hasPermission(eq(user), any(), any())).thenReturn(PermissionResult.denied(FailedLayer.ROLE));

        assertThatThrownBy(() -> service.receive(12, user)).isInstanceOf(PermissionDeniedException.class);
        when(alerts.lockForReceive(12)).thenReturn(Optional.of(state(AlertStatus.ESCALATED, 1)));
        assertThatThrownBy(() -> service.receive(12, user)).isInstanceOf(PermissionDeniedException.class);
        verifyNotReceived();

        when(alerts.lockForReceive(12)).thenReturn(Optional.of(state(AlertStatus.ESCALATED, 2)));
        service.receive(12, user);
        verify(alerts).receive(12, 900L, NOW, IncidentLevel.MEDIUM, 4);
    }

    @Test
    void FR_036_staffOutsideScopeSeesNotFound() {
        when(permissions.hasPermission(eq(user), any(), any())).thenReturn(PermissionResult.denied(FailedLayer.SCOPE));

        assertThatThrownBy(() -> service.receive(12, user)).isInstanceOf(NotFoundException.class);
        verifyNotReceived();
    }

    private void verifyNotReceived() {
        verify(alerts, never()).receive(anyLong(), anyLong(), any(), any(), anyInt());
    }
}
