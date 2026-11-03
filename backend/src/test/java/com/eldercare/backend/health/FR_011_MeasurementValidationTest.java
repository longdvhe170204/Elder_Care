package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.dto.*;
import com.eldercare.backend.health.internal.catalog.domain.*;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import com.eldercare.backend.health.internal.threshold.service.ApplicableThresholdResolver;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;

class FR_011_MeasurementValidationTest {
    private static final Instant NOW = Instant.parse("2026-11-02T08:00:00Z");
    private final VitalSignRecordStore records = mock(VitalSignRecordStore.class);
    private final CatalogApi catalogs = mock(CatalogApi.class);
    private final ParameterApi parameters = mock(ParameterApi.class);
    private final ApplicableThresholdResolver thresholds = mock(ApplicableThresholdResolver.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final VitalSignMeasurementService service = new VitalSignMeasurementService(
            records, catalogs, parameters, thresholds, time, events, permissions, audit);

    @BeforeEach
    void setup() {
        when(time.now()).thenReturn(NOW);
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(permissions.hasPermissionAt(any(), any(), any(), any()))
                .thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        when(catalogs.require(HealthCatalogs.VITAL_SIGN, 3L))
                .thenReturn(new CatalogItem<>(
                        3L,
                        "SPO2_TEST",
                        "Chỉ số kiểm thử",
                        null,
                        0,
                        CatalogStatus.ACTIVE,
                        new VitalSignAttributes("đơn vị", true),
                        null,
                        null,
                        0));
        when(parameters.read(ParameterCode.CFG_M06_03))
                .thenReturn(new ParameterValue(
                        ParameterCode.CFG_M06_03,
                        ParameterKind.TABLE,
                        JsonMapper.builder().build().readTree("{\"SPO2_TEST\":{\"VALUE\":{\"min\":0,\"max\":150}}}")));
        var original = mock(VitalSignRecord.class);
        when(original.confirmationStatus()).thenReturn(VitalSignConfirmationStatus.AWAITING_REMEASUREMENT);
        when(original.indicatorId()).thenReturn(3L);
        when(original.residentId()).thenReturn(12L);
        when(records.findById(5L)).thenReturn(Optional.of(original));
    }

    @Test
    void FR_011_physicalRangeComesOnlyFromCurrentParameter() {
        when(thresholds.resolve(anyLong(), eq(3L), anyString(), any()))
                .thenThrow(new IllegalStateException("threshold reached"));
        assertThatThrownBy(() -> service.record(
                        new RecordVitalSignCommand(
                                12L,
                                3L,
                                NOW,
                                42L,
                                Map.of("VALUE", new BigDecimal("120")),
                                false,
                                null,
                                null,
                                false,
                                null),
                        user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("threshold reached");
    }

    @Test
    void FR_016_emptyRemeasurementDoesNotConfirmOriginal() {
        assertThatThrownBy(() -> service.remeasure(new RemeasureVitalSignCommand(5L, NOW, 42L, Map.of(), null), user))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("EMPTY_VALUES"));
        verify(records, never()).confirmRemeasurement(anyLong(), any(), any());
        verifyNoInteractions(events);
    }

    @Test
    void FR_012_futureRemeasurementDoesNotConfirmOriginal() {
        assertThatThrownBy(() -> service.remeasure(
                        new RemeasureVitalSignCommand(
                                5L, NOW.plusSeconds(1), 42L, Map.of("VALUE", BigDecimal.TEN), null),
                        user))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("MEASURED_TIME_IN_FUTURE"));
        verify(records, never()).confirmRemeasurement(anyLong(), any(), any());
        verifyNoInteractions(events);
    }

    @Test
    void FR_010_permissionIsCheckedBeforeRecording() {
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.denied(FailedLayer.ROLE));
        assertThatThrownBy(() -> service.record(
                        new RecordVitalSignCommand(12L, 3L, NOW, 42L, Map.of(), false, null, null, false, null), user))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("PERMISSION_DENIED"));
        verifyNoInteractions(records, audit, events);
    }

    @Test
    void FR_016_remeasurementChecksScopeOfTheOriginalResident() {
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.denied(FailedLayer.SCOPE));
        assertThatThrownBy(() -> service.remeasure(
                        new RemeasureVitalSignCommand(5L, NOW, 42L, Map.of("VALUE", BigDecimal.TEN), null), user))
                .isInstanceOf(NotFoundException.class);
        verify(permissions)
                .hasPermission(
                        user,
                        new PermissionKey(Function.VITAL_SIGN, Operation.CREATE),
                        new Target("M06.VITAL_SIGN_RECORD", null, 12L, null));
        verify(records, never()).confirmRemeasurement(anyLong(), any(), any());
        verifyNoInteractions(audit, events);
    }

    @Test
    void FR_010_offlinePermissionUsesDeviceTimeEvenAfterCurrentPermissionIsLost() {
        when(permissions.hasPermission(any(), any(), any())).thenReturn(PermissionResult.denied(FailedLayer.ROLE));
        when(thresholds.resolve(anyLong(), eq(3L), anyString(), any()))
                .thenThrow(new IllegalStateException("historical permission accepted"));
        assertThatThrownBy(() -> service.recordOffline(
                        new RecordVitalSignCommand(
                                12L, 3L, NOW, 42L, Map.of("VALUE", BigDecimal.TEN), false, null, NOW, true, null),
                        user,
                        true))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("historical permission accepted");
        verify(permissions)
                .hasPermissionAt(
                        user,
                        new PermissionKey(Function.VITAL_SIGN, Operation.CREATE),
                        new Target("M06.VITAL_SIGN_RECORD", null, 12L, null),
                        NOW);
        verify(permissions, never()).hasPermission(any(), any(), any());
    }
}
