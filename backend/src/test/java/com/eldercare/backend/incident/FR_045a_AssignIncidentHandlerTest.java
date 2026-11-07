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

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.ApproverApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.AccountView;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.IncidentAssigned;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.domain.IncidentState;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentAssignmentService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/** Giao người xử lý sự cố (FR-045a, Q-339, Q-71; tasks T066). */
class FR_045a_AssignIncidentHandlerTest {
    private static final Instant NOW = Instant.parse("2026-11-04T05:00:00Z");
    private static final long NURSE = 700L;
    private static final long NURSE_STAFF_ID = 70L;

    private final IncidentStore incidents = mock(IncidentStore.class);
    private final IncidentProgressionStore records = mock(IncidentProgressionStore.class);
    private final ScopeApi scopes = mock(ScopeApi.class);
    private final ApproverApi approvers = mock(ApproverApi.class);
    private final AccountApi accounts = mock(AccountApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final IncidentAssignmentService service =
            new IncidentAssignmentService(incidents, records, scopes, approvers, accounts, audit, events, time);

    @BeforeEach
    void defaults() {
        when(time.now()).thenReturn(NOW);
        when(user.accountId()).thenReturn(900L);
        when(user.roles()).thenReturn(Set.of(Role.FLOOR_MANAGER));
        when(scopes.inScope(eq(user), any())).thenReturn(true);
        when(approvers.approvers(any(), any())).thenReturn(List.of(NURSE, 701L));
        when(accounts.hasRole(eq(NURSE), any(Role[].class))).thenReturn(true);
        when(accounts.byId(NURSE))
                .thenReturn(Optional.of(new AccountView(
                        NURSE, AccountType.STAFF, AccountStatus.ACTIVE, Set.of(Role.NURSING_STAFF), NURSE_STAFF_ID)));
        incident("NEW", null);
    }

    private void incident(String status, Long assignee) {
        when(incidents.lock(41))
                .thenReturn(Optional.of(new IncidentState(41, 101L, 7L, status, IncidentLevel.MEDIUM, false, 3)));
        when(incidents.assignee(41)).thenReturn(Optional.ofNullable(assignee));
    }

    @Test
    void FR_045a_floorManagerAssignsNewIncidentAndStopsEscalation() {
        service.assign(41, NURSE, "Điều dưỡng gần nhất", user);

        verify(incidents).assignFromNew(41, NURSE, 900L, NOW, 3);
        verify(records).recordAssignment(41, 900L, NURSE, "Điều dưỡng gần nhất", "NEW", NOW, 7L);
        var captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        var event = (IncidentAssigned) captor.getValue();
        assertThat(event.assigneeId()).isEqualTo(NURSE);
        assertThat(event.level()).isEqualTo("MEDIUM");
        assertThat(event.notificationRequests().getFirst().recipientGroups())
                .containsExactly(new RecipientGroup.Staff(NURSE_STAFF_ID, "INCIDENT_ASSIGNED"));
    }

    @Test
    void FR_045a_noteIsRequired() {
        assertThatThrownBy(() -> service.assign(41, NURSE, " ", user)).isInstanceOf(BusinessException.class);
        verifyNotAssigned();
    }

    @Test
    void FR_045a_assigneeMustBeNurseFloorManagerOrDoctorWithRightsOnResident() {
        when(accounts.hasRole(eq(701L), any(Role[].class))).thenReturn(false);
        assertThatThrownBy(() -> service.assign(41, 701L, "Giao", user)).isInstanceOf(ConditionNotMetException.class);
        assertThatThrownBy(() -> service.assign(41, 702L, "Giao", user)).isInstanceOf(ConditionNotMetException.class);
        verifyNotAssigned();
    }

    @Test
    void FR_045a_floorManagerOutsideScopeAndKitchenStaffAreRejected() {
        when(scopes.inScope(eq(user), any())).thenReturn(false);
        assertThatThrownBy(() -> service.assign(41, NURSE, "Giao", user)).isInstanceOf(PermissionDeniedException.class);

        when(user.roles()).thenReturn(Set.of(Role.KITCHEN_STAFF));
        when(scopes.inScope(eq(user), any())).thenReturn(true);
        assertThatThrownBy(() -> service.assign(41, NURSE, "Giao", user)).isInstanceOf(PermissionDeniedException.class);
        verifyNotAssigned();
    }

    @Test
    void Q_339_managerReassignsInProgressIncidentWithoutChangingStatus() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        when(scopes.inScope(eq(user), any())).thenReturn(false);
        incident("IN_PROGRESS", 701L);

        service.assign(41, NURSE, "Chuyển cho điều dưỡng ca chiều", user);

        verify(incidents).reassign(41, NURSE, 3);
        verify(incidents, never()).assignFromNew(anyLong(), anyLong(), anyLong(), any(), anyInt());
    }

    @Test
    void FR_045a_currentHandlerMayReassignButOnlyWhileHandling() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        when(user.accountId()).thenReturn(701L);
        incident("MONITORING", 701L);
        service.assign(41, NURSE, "Hết ca", user);
        verify(incidents).reassign(41, NURSE, 3);

        incident("NEW", null);
        assertThatThrownBy(() -> service.assign(41, NURSE, "Giao", user)).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_045a_assigneeMustDifferFromCurrentHandlerAndIncidentMustBeOpen() {
        incident("IN_PROGRESS", NURSE);
        assertThatThrownBy(() -> service.assign(41, NURSE, "Giao", user)).isInstanceOf(ConditionNotMetException.class);

        incident("CLOSED", 701L);
        assertThatThrownBy(() -> service.assign(41, NURSE, "Giao", user)).isInstanceOf(ConditionNotMetException.class);
        verifyNotAssigned();
    }

    private void verifyNotAssigned() {
        verify(incidents, never()).assignFromNew(anyLong(), anyLong(), anyLong(), any(), anyInt());
        verify(incidents, never()).reassign(anyLong(), anyLong(), anyInt());
    }
}
