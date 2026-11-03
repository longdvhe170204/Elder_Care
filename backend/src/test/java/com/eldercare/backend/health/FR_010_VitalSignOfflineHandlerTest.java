package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.common.offline.SyncOutcome;
import com.eldercare.backend.common.offline.SyncResult;
import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import com.eldercare.backend.health.api.dto.RemeasureVitalSignCommand;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignOfflineContent;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import com.eldercare.backend.health.internal.measurement.service.VitalSignOfflineHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** FR-010: đồng bộ chỉ số dùng mốc thiết bị và giữ quyền theo người gửi. */
class FR_010_VitalSignOfflineHandlerTest {
    private static final Instant DEVICE_TIME = Instant.parse("2026-11-02T08:00:00Z");

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void FR_010_recordsOfflineVitalSignWithDeviceTime(boolean permissionLost) {
        var measurements = mock(VitalSignMeasurementService.class);
        var records = mock(VitalSignRecordStore.class);
        var user = mock(CurrentUser.class);
        when(user.accountId()).thenReturn(42L);
        var saved = mock(VitalSignRecord.class);
        when(saved.id()).thenReturn(77L);
        when(measurements.recordOffline(any(), same(user), eq(permissionLost))).thenReturn(saved);
        var handler = new VitalSignOfflineHandler(measurements, records, user);
        var content =
                new VitalSignOfflineContent(12L, 3L, Map.of("VALUE", new BigDecimal("36.8")), false, null, null, null);

        assertThat(handler.recordType()).isEqualTo("M06.BAN_GHI_CHI_SO");
        assertThat(handler.permissionKey().function()).isEqualTo(Function.VITAL_SIGN);
        assertThat(handler.permissionKey().operation()).isEqualTo(Operation.CREATE);
        assertThat(handler.target(content).residentId()).isEqualTo(12L);
        assertThat(handler.requiresOnline()).isFalse();
        assertThat(handler.apply(content, outcome(permissionLost), DEVICE_TIME)).isEqualTo("vital_sign:77");
        verify(measurements)
                .recordOffline(
                        new RecordVitalSignCommand(
                                12L, 3L, DEVICE_TIME, 42L, content.values(), false, null, DEVICE_TIME, true, null),
                        user,
                        permissionLost);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void FR_010_remeasurementUsesOriginalRecordAndDeviceTime(boolean permissionLost) {
        var measurements = mock(VitalSignMeasurementService.class);
        var records = mock(VitalSignRecordStore.class);
        var user = mock(CurrentUser.class);
        when(user.accountId()).thenReturn(42L);
        var original = mock(VitalSignRecord.class);
        when(original.residentId()).thenReturn(12L);
        when(original.indicatorId()).thenReturn(3L);
        when(records.findById(5L)).thenReturn(java.util.Optional.of(original));
        var saved = mock(VitalSignRecord.class);
        when(saved.id()).thenReturn(78L);
        when(measurements.remeasureOffline(any(), same(user), same(DEVICE_TIME), eq(permissionLost)))
                .thenReturn(saved);
        var handler = new VitalSignOfflineHandler(measurements, records, user);
        var content =
                new VitalSignOfflineContent(12L, 3L, Map.of("VALUE", new BigDecimal("36.8")), false, null, null, 5L);

        handler.validate(content);
        assertThat(handler.apply(content, outcome(permissionLost), DEVICE_TIME)).isEqualTo("vital_sign:78");
        verify(measurements)
                .remeasureOffline(
                        new RemeasureVitalSignCommand(5L, DEVICE_TIME, 42L, content.values(), null),
                        user,
                        DEVICE_TIME,
                        permissionLost);
    }

    private static SyncOutcome outcome(boolean permissionLost) {
        return new SyncOutcome(
                permissionLost ? SyncResult.ACCEPTED_WITH_FLAG : SyncResult.ACCEPTED,
                permissionLost ? "PERMISSION_LOST_BEFORE_SYNC" : null);
    }
}
