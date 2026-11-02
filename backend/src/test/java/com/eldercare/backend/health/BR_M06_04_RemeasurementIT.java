package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import com.eldercare.backend.health.api.dto.RemeasureVitalSignCommand;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLabel;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-M06-04: Giá trị nguy hiểm chờ đo lại, đo lại một lần, xử lý ngay bỏ qua đo lại.
 */
@Transactional
class BR_M06_04_RemeasurementIT extends com.eldercare.backend.health.VitalSignMeasurementTestBase {

    private static final Instant MEASURED_AT = Instant.parse("2026-11-02T08:00:00Z");

    @Autowired
    VitalSignMeasurementService service;

    @Autowired
    VitalSignRecordStore records;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    ParameterApi parameters;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    private long indicatorId;
    private CurrentUser nurseUser;

    @BeforeEach
    void setup() {
        indicatorId = createIndicator("SPO2_TEST", "SpO2");
        com.eldercare.backend.health.VitalSignTestThresholds.seed(jdbc, indicatorId);
        nurseUser = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user();
        asMeasurementUser(nurseUser);
    }

    @Test
    void BR_M06_04_dangerousValueWithoutImmediateActionEntersAwaitingRemeasurement() {
        long residentId = TestUsers.nextId();
        var command = new RecordVitalSignCommand(
                residentId,
                indicatorId,
                MEASURED_AT,
                nurseUser.accountId(),
                Map.of("VALUE", new BigDecimal("88.00")),
                false,
                null,
                null,
                false,
                "Bệnh nhân khó thở nhẹ");

        var record = service.record(command, nurseUser);

        assertThat(record.confirmationStatus()).isEqualTo(VitalSignConfirmationStatus.AWAITING_REMEASUREMENT);
        assertThat(record.level()).isEqualTo(VitalSignLevel.DANGEROUS);
        assertThat(record.remeasurementDeadline()).isEqualTo(MEASURED_AT.plus(Duration.ofMinutes(10)));
        assertThat(record.confirmedAt()).isNull();
        assertThat(record.lockedAt()).isNull();

        var loaded = records.findById(record.id()).orElseThrow();
        assertThat(loaded.confirmationStatus()).isEqualTo(VitalSignConfirmationStatus.AWAITING_REMEASUREMENT);
        assertThat(loaded.lockedAt()).isNull();
    }

    @Test
    void BR_M06_04_dangerousValueWithImmediateActionConfirmsImmediately() {
        long residentId = TestUsers.nextId();
        var command = new RecordVitalSignCommand(
                residentId,
                indicatorId,
                MEASURED_AT,
                nurseUser.accountId(),
                Map.of("VALUE", new BigDecimal("88.00")),
                true,
                null,
                null,
                false,
                "Đã xử lý oxy ngay");

        var record = service.record(command, nurseUser);

        assertThat(record.confirmationStatus()).isEqualTo(VitalSignConfirmationStatus.CONFIRMED);
        assertThat(record.level()).isEqualTo(VitalSignLevel.DANGEROUS);
        assertThat(record.labels()).contains(VitalSignLabel.IMMEDIATE_ACTION);
        assertThat(record.confirmedAt()).isNotNull();
        assertThat(record.lockedAt()).isEqualTo(record.confirmedAt());

        var loaded = records.findById(record.id()).orElseThrow();
        assertThat(loaded.confirmationStatus()).isEqualTo(VitalSignConfirmationStatus.CONFIRMED);
        assertThat(loaded.lockedAt()).isNotNull();
    }

    @Test
    void BR_M06_04_remeasuringUpdatesOriginalToRemeasuredAndCreatesConfirmedRecord() {
        long residentId = TestUsers.nextId();
        var firstCommand = new RecordVitalSignCommand(
                residentId,
                indicatorId,
                MEASURED_AT,
                nurseUser.accountId(),
                Map.of("VALUE", new BigDecimal("88.00")),
                false,
                null,
                null,
                false,
                null);
        var original = service.record(firstCommand, nurseUser);

        Instant remeasuredAt = MEASURED_AT.plus(Duration.ofMinutes(5));
        var remeasureCommand = new RemeasureVitalSignCommand(
                original.id(),
                remeasuredAt,
                nurseUser.accountId(),
                Map.of("VALUE", new BigDecimal("96.00")),
                "Đo lại sau 5 phút");

        var remeasured = service.remeasure(remeasureCommand, nurseUser);

        assertThat(remeasured.confirmationStatus()).isEqualTo(VitalSignConfirmationStatus.CONFIRMED);
        assertThat(remeasured.level()).isEqualTo(VitalSignLevel.NORMAL);
        assertThat(remeasured.remeasurementFor()).isEqualTo(original.id());
        assertThat(remeasured.confirmedAt()).isNotNull();

        var updatedOriginal = records.findById(original.id()).orElseThrow();
        assertThat(updatedOriginal.confirmationStatus()).isEqualTo(VitalSignConfirmationStatus.REMEASURED);
        assertThat(updatedOriginal.confirmedAt()).isNotNull();
        assertThat(updatedOriginal.lockedAt()).isEqualTo(updatedOriginal.confirmedAt());
    }

    @Test
    void BR_M06_04_duplicateRemeasurementAttemptFails() {
        long residentId = TestUsers.nextId();
        var firstCommand = new RecordVitalSignCommand(
                residentId,
                indicatorId,
                MEASURED_AT,
                nurseUser.accountId(),
                Map.of("VALUE", new BigDecimal("88.00")),
                false,
                null,
                null,
                false,
                null);
        var original = service.record(firstCommand, nurseUser);

        Instant remeasuredAt = MEASURED_AT.plus(Duration.ofMinutes(5));
        var remeasureCommand = new RemeasureVitalSignCommand(
                original.id(),
                remeasuredAt,
                nurseUser.accountId(),
                Map.of("VALUE", new BigDecimal("96.00")),
                "Đo lại lần 1");
        service.remeasure(remeasureCommand, nurseUser);

        var secondRemeasureCommand = new RemeasureVitalSignCommand(
                original.id(),
                remeasuredAt.plusSeconds(30),
                nurseUser.accountId(),
                Map.of("VALUE", new BigDecimal("97.00")),
                "Đo lại lần 2");

        assertThatThrownBy(() -> service.remeasure(secondRemeasureCommand, nurseUser))
                .isInstanceOf(BusinessException.class);
    }

    private long createIndicator(String code, String name) {
        return catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>(code, name, null, 0, Map.of("unit", "%", "vitalSign", true)))
                .id();
    }
}
