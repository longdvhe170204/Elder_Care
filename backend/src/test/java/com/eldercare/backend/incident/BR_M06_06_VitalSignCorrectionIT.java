package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CorrectionView;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.VitalSignMeasurementTestBase;
import com.eldercare.backend.health.VitalSignTestThresholds;
import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.api.events.VitalSignCorrected;
import com.eldercare.backend.health.internal.measurement.service.VitalSignCorrectionProvider;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import com.eldercare.backend.incident.api.AlertApi;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * BR-M06-06, FR-019 (US10): bản ghi chỉ số đã xác nhận không sửa được; đính chính phát {@code VitalSignCorrected} kèm
 * mức hiện hành tính lại, và cảnh báo đã tạo không tự đóng.
 */
@RecordApplicationEvents
class BR_M06_06_VitalSignCorrectionIT extends VitalSignMeasurementTestBase {
    private static final Instant MEASURED_AT = Instant.parse("2026-11-02T08:00:00Z");

    @Autowired
    VitalSignMeasurementService measurementService;

    @Autowired
    VitalSignCorrectionProvider provider;

    @Autowired
    AlertApi alertApi;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    ApplicationEvents events;

    private long indicatorId;
    private long residentId;
    private CurrentUser nurse;
    private long recordId;

    @BeforeEach
    void setup() {
        long fixture = TestUsers.nextId();
        indicatorId = transactions.execute(status -> catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>(
                                "SPO2_CORR_TEST_" + fixture,
                                "SpO2 " + fixture,
                                null,
                                0,
                                Map.of("unit", "%", "vitalSign", true)))
                .id());
        VitalSignTestThresholds.seed(jdbc, indicatorId);
        nurse = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user();
        asMeasurementUser(nurse);
        residentId = TestUsers.nextId();
        var command = new RecordVitalSignCommand(
                residentId,
                indicatorId,
                MEASURED_AT,
                nurse.accountId(),
                Map.of("VALUE", new BigDecimal("92.00")),
                false,
                null,
                null,
                false,
                null);
        recordId = transactions
                .execute(status -> measurementService.record(command, nurse))
                .id();
    }

    @Test
    void FR_019_confirmedRecordCannotBeUpdated() {
        assertThatThrownBy(() -> jdbc.update("UPDATE vital_sign_record SET note = 'sửa' WHERE id = ?", recordId))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void FR_019_correctionPublishesRecalculatedLevelAndOriginalStaysReadable() {
        assertThat(provider.isConfirmed(recordId)).isTrue();
        assertThat(provider.originalContent(recordId))
                .extracting(content -> ((Map<?, ?>) ((Map<?, ?>) content).get("values")).get("VALUE"))
                .satisfies(value -> assertThat((BigDecimal) value).isEqualByComparingTo("92"));

        transactions.executeWithoutResult(status -> provider.afterCorrection(correction(Map.of("VALUE", "98"))));

        var published = events.stream(VitalSignCorrected.class).toList();
        assertThat(published).singleElement().satisfies(event -> {
            assertThat(event.originalRecordId()).isEqualTo(recordId);
            assertThat(event.originalLevel()).isEqualTo(VitalSignLevel.WARNING);
            assertThat(event.currentLevel()).isEqualTo(VitalSignLevel.NORMAL);
        });
    }

    @Test
    void FR_019_correctionDoesNotCloseTheAlertCreatedFromTheOriginal() {
        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(
                        () -> assertThat(alertApi.hasOpenAlert(residentId)).isTrue());

        transactions.executeWithoutResult(status -> provider.afterCorrection(correction(Map.of("VALUE", "98"))));

        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(8))
                .untilAsserted(
                        () -> assertThat(alertApi.hasOpenAlert(residentId)).isTrue());
    }

    private CorrectionView correction(Map<String, String> values) {
        return new CorrectionView(
                TestUsers.nextId(),
                "M06.VITAL_SIGN_RECORD",
                recordId,
                1,
                CorrectionType.AMEND_CONTENT,
                Map.of("values", values),
                "Ghi nhầm",
                nurse.accountId(),
                CreationMethod.A,
                null,
                time.now());
    }
}
