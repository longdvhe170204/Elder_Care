package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.AlertConversionService;
import com.eldercare.backend.incident.internal.alert.service.AlertHandlingService;
import com.eldercare.backend.incident.internal.incident.domain.RecordedIncident;
import com.eldercare.backend.incident.internal.incident.service.IncidentRecordService;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Chuyển sự cố từ cảnh báo Đang xử lý (bảng FR-029, FR-047i; US3 #8). */
class FR_029_AlertConversionTest {
    private static final Instant NOW = Instant.parse("2026-11-04T03:00:00Z");
    private static final long ID = 12;

    private final AlertStore alerts = mock(AlertStore.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final IncidentRecordService incidents = mock(IncidentRecordService.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final AlertConversionService service = new AlertConversionService(
            new AlertHandlingService(
                    alerts,
                    permissions,
                    mock(ApproverApi.class),
                    mock(AccountApi.class),
                    mock(AuditLogApi.class),
                    event -> {},
                    mock(ParameterApi.class),
                    time,
                    null,
                    null),
            alerts,
            incidents,
            mock(AuditLogApi.class),
            time,
            null);

    @BeforeEach
    void defaults() {
        when(time.now()).thenReturn(NOW);
        when(user.accountId()).thenReturn(900L);
        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(incidents.record(any(), any(), anyLong())).thenReturn(new RecordedIncident(55L, false, false));
        lock(AlertStatus.IN_PROGRESS, 101L);
    }

    @Test
    void US3_8_convertCreatesLinkedIncidentWithAlertLevelAndMarksAlert() {
        long incidentId = service.convert(ID, 3L, null, "Đã gọi bác sĩ", null, user);

        assertThat(incidentId).isEqualTo(55L);
        var command = ArgumentCaptor.forClass(RecordIncidentCommand.class);
        verify(incidents).record(command.capture(), eq(user), eq(ID));
        assertThat(command.getValue().level()).isEqualTo("MEDIUM");
        assertThat(command.getValue().residentId()).isEqualTo(101L);
        assertThat(command.getValue().description()).isEqualTo("Nhịp tim cao");
        verify(alerts).convert(ID, 55L, 3);
    }

    @Test
    void FR_029_onlyInProgressAlertCanConvert() {
        lock(AlertStatus.RECEIVED, 101L);
        assertThatThrownBy(() -> service.convert(ID, 3L, null, null, null, user))
                .isInstanceOf(ConditionNotMetException.class);
        verify(incidents, never()).record(any(), any(), anyLong());
    }

    @Test
    void FR_047i_alertWithoutResidentNeedsResidentInRequest() {
        lock(AlertStatus.IN_PROGRESS, null);
        assertThatThrownBy(() -> service.convert(ID, 3L, null, null, null, user))
                .isInstanceOf(InvalidValueException.class);

        service.convert(ID, 3L, null, null, 202L, user);
        var command = ArgumentCaptor.forClass(RecordIncidentCommand.class);
        verify(incidents).record(command.capture(), eq(user), eq(ID));
        assertThat(command.getValue().residentId()).isEqualTo(202L);
    }

    @Test
    void FR_029_onlyNurseFloorManagerOrDoctorConverts() {
        when(user.roles()).thenReturn(Set.of(Role.OFFICE_STAFF));
        assertThatThrownBy(() -> service.convert(ID, 3L, null, null, null, user))
                .isInstanceOf(PermissionDeniedException.class);
    }

    private void lock(AlertStatus status, Long residentId) {
        when(alerts.lock(ID))
                .thenReturn(Optional.of(new AlertRecord(
                        ID,
                        residentId,
                        7L,
                        "CHI_SO",
                        "VUOT_NGUONG:5",
                        IncidentLevel.MEDIUM,
                        null,
                        1,
                        NOW,
                        NOW,
                        status,
                        0,
                        null,
                        null,
                        0,
                        900L,
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
                        "Nhịp tim cao",
                        null,
                        null,
                        null,
                        false,
                        3)));
    }
}
