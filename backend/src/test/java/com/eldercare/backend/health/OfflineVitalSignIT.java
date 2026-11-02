package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.offline.SyncOutcome;
import com.eldercare.backend.common.offline.SyncResult;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLabel;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignOfflineContent;
import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import com.eldercare.backend.health.internal.measurement.service.VitalSignOfflineHandler;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi nhận chỉ số ngoại tuyến (FR-010, S-06): lưu thoi_diem_thiet_bi, nhãn OFFLINE_RECORDED.
 */
@Transactional
class OfflineVitalSignIT extends com.eldercare.backend.health.VitalSignMeasurementTestBase {

    @Autowired
    VitalSignMeasurementService service;

    @Autowired
    VitalSignRecordStore records;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    private long indicatorId;
    private CurrentUser caregiverUser;

    @BeforeEach
    void setup() {
        indicatorId = catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>("TEMP_TEST", "Nhiệt độ", null, 0, Map.of("unit", "°C", "vitalSign", true)))
                .id();
        caregiverUser = new TestUsers(jdbc, time).staff(Role.CAREGIVER).user();
        asMeasurementUser(caregiverUser);
    }

    @Test
    void offlineMeasurement_persistsDeviceMeasuredAtAndOfflineRecordedLabel() {
        long residentId = TestUsers.nextId();
        Instant deviceTime = Instant.parse("2026-11-02T07:30:00Z");
        Instant syncTime = Instant.parse("2026-11-02T08:00:00Z");

        var command = new RecordVitalSignCommand(
                residentId,
                indicatorId,
                syncTime,
                caregiverUser.accountId(),
                Map.of("VALUE", new BigDecimal("36.80")),
                false,
                null,
                deviceTime,
                true,
                "Đồng bộ từ máy đo nhiệt độ bluetooth");

        var record = service.record(command, caregiverUser);

        assertThat(record.confirmationStatus()).isEqualTo(VitalSignConfirmationStatus.CONFIRMED);
        assertThat(record.level()).isEqualTo(VitalSignLevel.NO_THRESHOLD);
        assertThat(record.labels()).contains(VitalSignLabel.OFFLINE_RECORDED);
        assertThat(record.deviceMeasuredAt()).isEqualTo(deviceTime);
        assertThat(record.offline()).isTrue();

        var loaded = records.findById(record.id()).orElseThrow();
        assertThat(loaded.labels()).contains(VitalSignLabel.OFFLINE_RECORDED);
        assertThat(loaded.deviceMeasuredAt()).isEqualTo(deviceTime);
        assertThat(loaded.offline()).isTrue();
    }

    @Test
    void FR_010_syncAfterPermissionLossIsMarkedOnStoredRecord() {
        long residentId = TestUsers.nextId();
        Instant deviceTime = Instant.parse("2026-11-02T07:30:00Z");
        var command = new RecordVitalSignCommand(
                residentId,
                indicatorId,
                deviceTime,
                caregiverUser.accountId(),
                Map.of("VALUE", new BigDecimal("36.80")),
                false,
                null,
                deviceTime,
                true,
                null);

        var record = service.recordOffline(command, caregiverUser, true);

        assertThat(jdbc.<Boolean>queryForObject(
                        "SELECT synced_after_permission_loss FROM vital_sign_record WHERE id = ?",
                        Boolean.class,
                        record.id()))
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void FR_010_offlineFirstAndRepeatMeasurementsPersistPermissionFlag(boolean permissionLost) {
        long residentId = TestUsers.nextId();
        long spo2Id = catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>(
                                "SPO2_OFFLINE_" + residentId,
                                "SpO2 ngoại tuyến",
                                null,
                                0,
                                Map.of("unit", "%", "vitalSign", true)))
                .id();
        VitalSignTestThresholds.seed(jdbc, spo2Id);
        Instant deviceTime = time.now().minusSeconds(300).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var outcome = new SyncOutcome(
                permissionLost ? SyncResult.ACCEPTED_WITH_FLAG : SyncResult.ACCEPTED,
                permissionLost ? "PERMISSION_LOST_BEFORE_SYNC" : null);
        var handler = new VitalSignOfflineHandler(service, records, caregiverUser);
        var first = new VitalSignOfflineContent(
                residentId, spo2Id, Map.of("VALUE", new BigDecimal("88.00")), false, null, null, null);
        handler.validate(first);
        long firstId = Long.parseLong(handler.apply(first, outcome, deviceTime).substring("vital_sign:".length()));
        assertThat(records.findById(firstId).orElseThrow().confirmationStatus())
                .isEqualTo(VitalSignConfirmationStatus.AWAITING_REMEASUREMENT);

        Instant repeatTime = deviceTime.plusSeconds(60);
        var repeat = new VitalSignOfflineContent(
                residentId, spo2Id, Map.of("VALUE", new BigDecimal("96.00")), false, null, null, firstId);
        handler.validate(repeat);
        long repeatId =
                Long.parseLong(handler.apply(repeat, outcome, repeatTime).substring("vital_sign:".length()));
        var original = records.findById(firstId).orElseThrow();
        var remeasured = records.findById(repeatId).orElseThrow();
        assertThat(original.confirmationStatus()).isEqualTo(VitalSignConfirmationStatus.REMEASURED);
        assertThat(remeasured.confirmationStatus()).isEqualTo(VitalSignConfirmationStatus.CONFIRMED);
        assertThat(remeasured.remeasurementFor()).isEqualTo(firstId);
        assertThat(original.deviceMeasuredAt()).isEqualTo(deviceTime);
        assertThat(remeasured.deviceMeasuredAt()).isEqualTo(repeatTime);
        assertThat(original.measuredAt()).isEqualTo(deviceTime);
        assertThat(remeasured.measuredAt()).isEqualTo(repeatTime);
        assertThat(original.labels()).contains(VitalSignLabel.OFFLINE_RECORDED);
        assertThat(remeasured.labels()).contains(VitalSignLabel.OFFLINE_RECORDED);
        for (long id : new long[] {firstId, repeatId}) {
            assertThat(jdbc.<Boolean>queryForObject(
                            "SELECT synced_after_permission_loss FROM vital_sign_record WHERE id = ?",
                            Boolean.class,
                            id))
                    .isEqualTo(permissionLost);
        }
    }
}
