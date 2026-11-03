package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import com.eldercare.backend.incident.api.AlertApi;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * BR-M06-03: Chỉ số vượt ngưỡng tạo hoặc gộp vào cảnh báo theo dõi (FR-017, FR-028, S-08).
 */
class BR_M06_03_CreateAlertIT extends com.eldercare.backend.health.VitalSignMeasurementTestBase {

    private static final Instant MEASURED_AT = Instant.parse("2026-11-02T08:00:00Z");

    @Autowired
    VitalSignMeasurementService measurementService;

    @Autowired
    AlertApi alertApi;

    @Autowired
    AlertStore alertStore;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TransactionTemplate transactions;

    private long indicatorId;
    private CurrentUser nurseUser;
    private long residentId;

    @BeforeEach
    void setup() {
        long fixture = TestUsers.nextId();
        indicatorId = transactions.execute(status -> catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>(
                                "SPO2_ALERT_TEST_" + fixture,
                                "SpO2 Alert " + fixture,
                                null,
                                0,
                                Map.of("unit", "%", "vitalSign", true)))
                .id());
        com.eldercare.backend.health.VitalSignTestThresholds.seed(jdbc, indicatorId);
        nurseUser = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user();
        asMeasurementUser(nurseUser);
        residentId = TestUsers.nextId();
    }

    @Test
    void BR_M06_03_vitalSignConfirmedWithWarningCreatesAlert() {
        // SpO2 = 92 (cảnh báo: < 94, >= 90) -> CONFIRMED -> phát VitalSignConfirmed -> tạo Alert
        record(new RecordVitalSignCommand(
                residentId,
                indicatorId,
                MEASURED_AT,
                nurseUser.accountId(),
                Map.of("VALUE", new BigDecimal("92.00")),
                false,
                null,
                null,
                false,
                null));

        // Kiểm tra cảnh báo mở đã được tạo cho người cao tuổi
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> assertThat(alertApi.hasOpenAlert(residentId)).isTrue());
        var alerts = alertApi.byResident(residentId, MEASURED_AT.minusSeconds(60), MEASURED_AT.plusSeconds(60));
        assertThat(alerts).hasSize(1);
        assertThat(alerts.getFirst().level()).isEqualTo(IncidentLevel.MEDIUM.name());
        assertThat(alerts.getFirst().type()).isEqualTo("CHI_SO");

        var record = alertStore.findById(alerts.getFirst().id()).orElseThrow();
        assertThat(record.occurrenceCount()).isEqualTo(1);
    }

    @Test
    void BR_M06_03_repeatedWarningMergesAndRaisesLevel() {
        // Lần 1: SpO2 = 92 (cảnh báo) -> tạo Alert mức MEDIUM
        record(new RecordVitalSignCommand(
                residentId,
                indicatorId,
                MEASURED_AT,
                nurseUser.accountId(),
                Map.of("VALUE", new BigDecimal("92.00")),
                false,
                null,
                null,
                false,
                null));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(alertApi.byResident(
                                residentId, MEASURED_AT.minusSeconds(60), MEASURED_AT.plusSeconds(60)))
                        .hasSize(1));

        var firstAlert = alertApi.byResident(residentId, MEASURED_AT.minusSeconds(60), MEASURED_AT.plusSeconds(60))
                .getFirst();
        assertThat(firstAlert.level()).isEqualTo(IncidentLevel.MEDIUM.name());

        // Lần 2: SpO2 = 93 (cảnh báo) -> gộp vào Alert đang mở, tăng số lần
        Instant measuredAt2 = MEASURED_AT.plusSeconds(300);
        record(new RecordVitalSignCommand(
                residentId,
                indicatorId,
                measuredAt2,
                nurseUser.accountId(),
                Map.of("VALUE", new BigDecimal("93.00")),
                false,
                null,
                null,
                false,
                null));

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(alertStore
                                .findById(firstAlert.id())
                                .orElseThrow()
                                .occurrenceCount())
                        .isEqualTo(2));
        var recordAfterSecond = alertStore.findById(firstAlert.id()).orElseThrow();
        assertThat(recordAfterSecond.occurrenceCount()).isEqualTo(2);
        assertThat(recordAfterSecond.level()).isEqualTo(IncidentLevel.MEDIUM);

        // Lần 3: SpO2 = 88 (nguy hiểm, có xử lý ngay -> xác nhận ngay) -> nâng mức lên CRITICAL!
        Instant measuredAt3 = MEASURED_AT.plusSeconds(600);
        record(new RecordVitalSignCommand(
                residentId,
                indicatorId,
                measuredAt3,
                nurseUser.accountId(),
                Map.of("VALUE", new BigDecimal("88.00")),
                true, // immediate action
                null,
                null,
                false,
                null));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var alert = alertStore.findById(firstAlert.id()).orElseThrow();
            assertThat(alert.occurrenceCount()).isEqualTo(3);
            assertThat(alert.level()).isEqualTo(IncidentLevel.CRITICAL);
        });
        var recordAfterThird = alertStore.findById(firstAlert.id()).orElseThrow();
        assertThat(recordAfterThird.occurrenceCount()).isEqualTo(3);
        assertThat(recordAfterThird.level()).isEqualTo(IncidentLevel.CRITICAL);
    }

    private void record(RecordVitalSignCommand command) {
        transactions.executeWithoutResult(status -> measurementService.record(command, nurseUser));
    }
}
