package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.CreateAlertCommand;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeView;
import com.eldercare.backend.incident.internal.alert.service.AlertResponsibility;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.alert.service.ManualAlertService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Tạo cảnh báo thủ công (FR-026 (c), FR-028, FR-030, bảng FR-029 dòng Tạo). */
class FR_026_ManualAlertTest {
    private static final Instant NOW = Instant.parse("2026-11-04T03:00:00Z");

    private final AlertService alerts = mock(AlertService.class);
    private final IncidentCatalogService catalog = mock(IncidentCatalogService.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final AlertResponsibility responsibility = mock(AlertResponsibility.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final ManualAlertService service =
            new ManualAlertService(alerts, catalog, permissions, audit, time, responsibility);

    @BeforeEach
    void defaults() {
        when(time.now()).thenReturn(NOW);
        when(user.accountId()).thenReturn(900L);
        when(permissions.hasPermission(eq(user), any(), any()))
                .thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(catalog.incidentTypes())
                .thenReturn(List.of(
                        new IncidentTypeView(
                                30,
                                "ABNORMAL_BEHAVIOR",
                                "Hành vi bất thường",
                                IncidentLevel.LOW,
                                false,
                                false,
                                true,
                                0),
                        new IncidentTypeView(31, "OLD", "Ngừng dùng", IncidentLevel.LOW, false, false, false, 0)));
        when(responsibility.floorOf(101, NOW)).thenReturn(Optional.of(7L));
        when(alerts.requestAlert(any())).thenReturn(Optional.of(alert(1)));
    }

    @Test
    void FR_026_manualAlertUsesManualTypeLockAndDescription() {
        var result = service.create(101, 30, "MEDIUM", "Đi lại liên tục, nói lẫn", user);

        var command = ArgumentCaptor.forClass(CreateAlertCommand.class);
        verify(alerts).requestAlert(command.capture());
        assertThat(command.getValue().typeLock()).isEqualTo("THU_CONG:30");
        assertThat(command.getValue().type()).isEqualTo("ABNORMAL_BEHAVIOR");
        assertThat(command.getValue().level()).isEqualTo(IncidentLevel.MEDIUM);
        assertThat(command.getValue().floorAreaId()).isEqualTo(7L);
        assertThat(command.getValue().createdById()).isEqualTo(900L);
        assertThat(command.getValue().description()).isEqualTo("Đi lại liên tục, nói lẫn");
        assertThat(result.merged()).isFalse();
        verify(audit).write(any());
    }

    @Test
    void FR_030_secondManualAlertOfSameTypeIsMerged() {
        when(alerts.requestAlert(any())).thenReturn(Optional.of(alert(2)));

        assertThat(service.create(101, 30, "MEDIUM", "Lần nữa", user).merged()).isTrue();
    }

    @Test
    void FR_026_descriptionRequired() {
        assertThatThrownBy(() -> service.create(101, 30, "MEDIUM", "  ", user))
                .isInstanceOf(InvalidValueException.class);
        verify(alerts, never()).requestAlert(any());
    }

    private static AlertRecord alert(int count) {
        return new AlertRecord(
                40,
                101L,
                7L,
                "ABNORMAL_BEHAVIOR",
                "THU_CONG:30",
                IncidentLevel.MEDIUM,
                null,
                count,
                NOW,
                NOW,
                AlertStatus.NEW,
                0,
                NOW.plusSeconds(900),
                null,
                0,
                800L,
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
                900L,
                "Mô tả",
                null,
                null,
                null,
                false,
                1);
    }
}
