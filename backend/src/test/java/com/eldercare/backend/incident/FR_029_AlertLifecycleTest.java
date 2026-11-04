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
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.AccountView;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.AlertLevelRaised;
import com.eldercare.backend.incident.api.events.AssigneeChanged;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.AlertHandlingService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Lệnh vòng đời cảnh báo ngoài Tiếp nhận (bảng FR-029, FR-031, FR-036, Q-71, BR-M04-06). */
class FR_029_AlertLifecycleTest {
    private static final Instant NOW = Instant.parse("2026-11-04T03:00:00Z");
    private static final long ID = 12;
    private static final Reason REASON = new Reason("Lặp lại nhiều lần");

    private final AlertStore alerts = mock(AlertStore.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final ApproverApi approvers = mock(ApproverApi.class);
    private final AccountApi accounts = mock(AccountApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final ParameterApi parameters = mock(ParameterApi.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final List<Object> events = new ArrayList<>();
    private final AlertHandlingService service = new AlertHandlingService(
            alerts, permissions, approvers, accounts, audit, events::add, parameters, time, null, null);

    @BeforeEach
    void defaults() {
        when(time.now()).thenReturn(NOW);
        when(user.accountId()).thenReturn(900L);
        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        when(permissions.hasPermission(eq(user), any(), any()))
                .thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        var interval = mock(ParameterValue.class);
        when(interval.asDuration()).thenReturn(Duration.ofMinutes(15));
        when(parameters.read(ParameterCode.CFG_M05_01)).thenReturn(interval);
        lock(AlertStatus.RECEIVED, IncidentLevel.MEDIUM, 800L, "VUOT_NGUONG:5");
    }

    @Test
    void FR_029_startHandlingMovesReceivedToInProgressAndTakesOver() {
        service.startHandling(ID, user);

        verify(alerts).startHandling(ID, 900L, 3);
    }

    @Test
    void FR_029_startHandlingOnlyFromReceived() {
        lock(AlertStatus.NEW, IncidentLevel.MEDIUM, 800L, "VUOT_NGUONG:5");

        assertThatThrownBy(() -> service.startHandling(ID, user)).isInstanceOf(BusinessException.class);
        verify(alerts, never()).startHandling(anyLong(), anyLong(), anyInt());
    }

    @Test
    void Q_71_managerCannotStartHandlingCloseOrChangeLevel() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));

        assertThatThrownBy(() -> service.startHandling(ID, user)).isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> service.changeLevel(null, ID, IncidentLevel.CRITICAL, REASON, user))
                .isInstanceOf(PermissionDeniedException.class);
        lock(AlertStatus.IN_PROGRESS, IncidentLevel.MEDIUM, 900L, "VUOT_NGUONG:5");
        assertThatThrownBy(() -> service.close(ID, "Đã xử lý", null, user))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_036_outsideScopeIsNotFound() {
        when(permissions.hasPermission(eq(user), any(), any())).thenReturn(PermissionResult.denied(FailedLayer.SCOPE));

        assertThatThrownBy(() -> service.startHandling(ID, user)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void FR_029_raisingNewAlertRecomputesDeadlineAndNotifies() {
        lock(AlertStatus.NEW, IncidentLevel.LOW, 800L, "VUOT_NGUONG:5");

        service.changeLevel(null, ID, IncidentLevel.MEDIUM, REASON, user);

        verify(alerts).changeLevel(ID, IncidentLevel.MEDIUM, NOW.plus(Duration.ofMinutes(15)), 3);
        assertThat(events).singleElement().isInstanceOf(AlertLevelRaised.class);
        verify(audit).write(any());
    }

    @Test
    void FR_029_changeLevelAfterReceiveKeepsDeadlineAndLoweringDoesNotNotify() {
        lock(AlertStatus.RECEIVED, IncidentLevel.MEDIUM, 800L, "VUOT_NGUONG:5");

        service.changeLevel(null, ID, IncidentLevel.LOW, REASON, user);

        verify(alerts).changeLevel(ID, IncidentLevel.LOW, null, 3);
        assertThat(events).isEmpty();
    }

    @Test
    void FR_029_onlyDoctorLowersFromCritical() {
        lock(AlertStatus.RECEIVED, IncidentLevel.CRITICAL, 800L, "VUOT_NGUONG:5");

        assertThatThrownBy(() -> service.changeLevel(null, ID, IncidentLevel.MEDIUM, REASON, user))
                .isInstanceOf(PermissionDeniedException.class);

        when(user.roles()).thenReturn(Set.of(Role.DOCTOR));
        service.changeLevel(null, ID, IncidentLevel.MEDIUM, REASON, user);
        verify(alerts).changeLevel(ID, IncidentLevel.MEDIUM, null, 3);
    }

    @Test
    void FR_029_changeLevelNeedsDifferentLevel() {
        assertThatThrownBy(() -> service.changeLevel(null, ID, IncidentLevel.MEDIUM, REASON, user))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_029_closeNeedsInProgressAndResolution() {
        assertThatThrownBy(() -> service.close(ID, "Đã xử lý", null, user)).isInstanceOf(BusinessException.class);

        lock(AlertStatus.IN_PROGRESS, IncidentLevel.MEDIUM, 900L, "VUOT_NGUONG:5");
        assertThatThrownBy(() -> service.close(ID, " ", null, user)).isInstanceOf(InvalidValueException.class);

        service.close(ID, "Đã cho nghỉ, đo lại bình thường", null, user);
        verify(alerts).close(ID, "Đã cho nghỉ, đo lại bình thường", null, 900L, NOW, 3);
    }

    @Test
    void BR_M04_06_overdueMandatoryTaskAlertNeedsReason() {
        lock(AlertStatus.IN_PROGRESS, IncidentLevel.MEDIUM, 900L, "QUA_HAN:44");

        assertThatThrownBy(() -> service.close(ID, "Đã làm", null, user)).isInstanceOf(BusinessException.class);

        service.close(ID, "Đã làm", REASON, user);
        verify(alerts).close(ID, "Đã làm", REASON.value(), 900L, NOW, 3);
    }

    @Test
    void FR_029_managerAssignsWhenAssigneeToEligibleStaffWithNote() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        lock(AlertStatus.RECEIVED, IncidentLevel.MEDIUM, 900L, "VUOT_NGUONG:5");
        when(accounts.byStaff(55)).thenReturn(Optional.of(account(950, 55)));
        when(approvers.approvers(any(), any())).thenReturn(List.of(950L));
        when(accounts.hasRole(eq(950L), any(Role[].class))).thenReturn(true);

        assertThatThrownBy(() -> service.assign(ID, 55, " ", user)).isInstanceOf(InvalidValueException.class);
        service.assign(ID, 55, "Giao cho D2 cùng tầng", user);

        verify(alerts).reassign(ID, 950L, 3);
        assertThat(events).singleElement().isInstanceOfSatisfying(AssigneeChanged.class, event -> {
            assertThat(event.newAssigneeId()).isEqualTo(950L);
            assertThat(event.notificationRequests()).hasSize(1);
        });
    }

    @Test
    void FR_029_onlyManagerWhoIsAssigneeCanAssign() {
        when(accounts.byStaff(55)).thenReturn(Optional.of(account(950, 55)));
        assertThatThrownBy(() -> service.assign(ID, 55, "Giao", user)).isInstanceOf(PermissionDeniedException.class);

        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        assertThatThrownBy(() -> service.assign(ID, 55, "Giao", user)).isInstanceOf(PermissionDeniedException.class);
        verify(alerts, never()).reassign(anyLong(), anyLong(), anyInt());
    }

    @Test
    void FR_029_assigneeMustHaveHandlingRightOnResident() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        lock(AlertStatus.RECEIVED, IncidentLevel.MEDIUM, 900L, "VUOT_NGUONG:5");
        when(accounts.byStaff(55)).thenReturn(Optional.of(account(950, 55)));
        when(approvers.approvers(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.assign(ID, 55, "Giao", user)).isInstanceOf(BusinessException.class);
        verify(alerts, never()).reassign(anyLong(), anyLong(), anyInt());
    }

    private static AccountView account(long accountId, long staffId) {
        return new AccountView(accountId, AccountType.STAFF, AccountStatus.ACTIVE, Set.of(Role.NURSING_STAFF), staffId);
    }

    private void lock(AlertStatus status, IncidentLevel level, Long assignee, String typeLock) {
        when(alerts.lock(ID))
                .thenReturn(Optional.of(new AlertRecord(
                        ID,
                        101L,
                        7L,
                        "CHI_SO",
                        typeLock,
                        level,
                        null,
                        1,
                        NOW,
                        NOW,
                        status,
                        0,
                        status == AlertStatus.NEW ? NOW.plusSeconds(60) : null,
                        null,
                        0,
                        assignee,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null,
                        false,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        3)));
    }
}
