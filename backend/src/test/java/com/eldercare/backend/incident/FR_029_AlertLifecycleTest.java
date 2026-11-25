package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.time.BusinessTime;
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
    void BR_19_overdueMandatoryTaskAlertNeedsReason() {
        when(user.roles()).thenReturn(Set.of(Role.FLOOR_MANAGER));
        lock(AlertStatus.IN_PROGRESS, IncidentLevel.MEDIUM, 900L, "QUA_HAN:44");

        assertThatThrownBy(() -> service.close(ID, "Đã làm", null, user)).isInstanceOf(BusinessException.class);

        service.close(ID, "Đã làm", REASON, user);
        verify(alerts).close(ID, "Đã làm", REASON.value(), 900L, NOW, 3);
    }

    /** Chốt BF-06 (2026-11-25): Điều dưỡng, Trưởng tầng, Bác sĩ đóng; Quản lý và Nhân viên chăm sóc không đóng cảnh báo sức khỏe. */
    @Test
    void BF_06_nurseFloorManagerDoctorClose_managerAndCaregiverDoNot() {
        lock(AlertStatus.IN_PROGRESS, IncidentLevel.MEDIUM, 900L, "VUOT_NGUONG:5");

        when(user.roles()).thenReturn(Set.of(Role.CAREGIVER));
        assertThatThrownBy(() -> service.close(ID, "Huyết áp ổn định", null, user))
                .isInstanceOf(PermissionDeniedException.class);
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        assertThatThrownBy(() -> service.close(ID, "Huyết áp ổn định", null, user))
                .isInstanceOf(PermissionDeniedException.class);

        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        service.close(ID, "Huyết áp ổn định", null, user);
        verify(alerts).close(ID, "Huyết áp ổn định", null, 900L, NOW, 3);
    }

    /** BF-06: nhân viên chăm sóc đóng cảnh báo nhắc việc chăm sóc mức Nhẹ, kể cả khi chưa ai tiếp nhận. */
    @Test
    void BF_06_caregiverClosesLowCareReminder() {
        when(user.roles()).thenReturn(Set.of(Role.CAREGIVER));
        lockOfType("LOW_WATER_INTAKE", AlertStatus.NEW, IncidentLevel.LOW, null, "LOW_WATER_INTAKE:101");

        service.close(ID, "Đã cho cụ uống nước", new Reason("Đã bù lúc 9h10"), user);

        verify(alerts).close(eq(ID), eq("Đã cho cụ uống nước"), any(), eq(900L), any(), anyInt());
    }

    @Test
    void BF_06_caregiverClosesMediumOverdueTaskWithReasonOnly() {
        when(user.roles()).thenReturn(Set.of(Role.CAREGIVER));
        lockOfType("OVERDUE_CARE_TASK", AlertStatus.NEW, IncidentLevel.MEDIUM, null, "OVERDUE_CARE_TASK:7");
        assertThatThrownBy(() -> service.close(ID, "Đã làm bù", null, user))
                .isInstanceOf(ReasonRequiredException.class);

        service.close(ID, "Đã làm bù", new Reason("Làm bù lúc 9h10"), user);

        verify(alerts).close(eq(ID), eq("Đã làm bù"), any(), eq(900L), any(), anyInt());
    }

    @Test
    void BF_06_caregiverCannotCloseCriticalReminderOrOtherTypes() {
        when(user.roles()).thenReturn(Set.of(Role.CAREGIVER));
        lockOfType("LOW_WATER_INTAKE", AlertStatus.NEW, IncidentLevel.CRITICAL, null, "LOW_WATER_INTAKE:101");
        assertThatThrownBy(() -> service.close(ID, "Đã làm", null, user)).isInstanceOf(PermissionDeniedException.class);

        lockOfType("CHI_SO", AlertStatus.NEW, IncidentLevel.LOW, null, "VUOT_NGUONG:5");
        assertThatThrownBy(() -> service.close(ID, "Đã làm", null, user)).isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void Q_442_floorManagerClosesWithoutTimeLimit() {
        when(user.roles()).thenReturn(Set.of(Role.FLOOR_MANAGER));
        lock(AlertStatus.IN_PROGRESS, IncidentLevel.MEDIUM, 900L, "VUOT_NGUONG:5");

        service.close(ID, "Huyết áp ổn định sau 2 lần đo", null, user);
        verify(alerts).close(ID, "Huyết áp ổn định sau 2 lần đo", null, 900L, NOW, 3);
    }

    @Test
    void FR_087_followUpKeepsStatusAndNeedsContent() {
        when(user.roles()).thenReturn(Set.of(Role.CAREGIVER));
        lock(AlertStatus.IN_PROGRESS, IncidentLevel.MEDIUM, 900L, "VUOT_NGUONG:5");

        assertThatThrownBy(() -> service.recordFollowUp(ID, " ", user)).isInstanceOf(BusinessException.class);
        service.recordFollowUp(ID, "Đo lại 150/95", user);
        verify(audit).write(any());
        verify(alerts, org.mockito.Mockito.never()).close(anyLong(), any(), any(), anyLong(), any(), anyInt());
    }

    @Test
    void FR_087_followUpBlockedForManagerAndClosedAlert() {
        when(user.roles()).thenReturn(Set.of(Role.NURSING_HOME_MANAGER));
        lock(AlertStatus.IN_PROGRESS, IncidentLevel.MEDIUM, 900L, "VUOT_NGUONG:5");
        assertThatThrownBy(() -> service.recordFollowUp(ID, "Ghi", user)).isInstanceOf(PermissionDeniedException.class);

        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        lock(AlertStatus.CLOSED, IncidentLevel.MEDIUM, 900L, "VUOT_NGUONG:5");
        assertThatThrownBy(() -> service.recordFollowUp(ID, "Ghi", user)).isInstanceOf(BusinessException.class);
    }

    private static AccountView account(long accountId, long staffId) {
        return new AccountView(accountId, AccountType.STAFF, AccountStatus.ACTIVE, Set.of(Role.NURSING_STAFF), staffId);
    }

    private void lock(AlertStatus status, IncidentLevel level, Long assignee, String typeLock) {
        lockOfType("CHI_SO", status, level, assignee, typeLock);
    }

    private void lockOfType(String type, AlertStatus status, IncidentLevel level, Long assignee, String typeLock) {
        when(alerts.lock(ID))
                .thenReturn(Optional.of(new AlertRecord(
                        ID,
                        101L,
                        7L,
                        type,
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
