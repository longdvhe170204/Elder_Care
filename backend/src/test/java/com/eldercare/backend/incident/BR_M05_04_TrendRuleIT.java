package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.dto.Importance;
import com.eldercare.backend.care.api.events.MeasurementTaskMissed;
import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.health.api.dto.LatestVitalSignView;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.api.dto.VitalSignReadingView;
import com.eldercare.backend.health.api.events.VitalSignConfirmed;
import com.eldercare.backend.health.api.events.VitalSignCorrected;
import com.eldercare.backend.incident.api.events.WeightLossDetected;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.TrendRuleView;
import com.eldercare.backend.incident.internal.alert.domain.TypeLock;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.trend.service.TrendRuleService;
import com.eldercare.backend.medication.api.events.MedicationAlertRequestedEvent;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * BR-M05-04, FR-039 → FR-039b: sụt cân (khi xác nhận và khi đính chính cân nặng), bỏ lỡ lần đo theo lịch và từ chối
 * cùng một thuốc tạo hoặc gộp cảnh báo xu hướng đúng loại và mức; quy tắc tắt thì không tạo. Listener chạy bất đồng bộ
 * sau commit nên test chờ kết quả. Mất ngủ kéo dài (FR-039c) chờ API đọc kết quả ngủ theo khoảng của 005.
 */
class BR_M05_04_TrendRuleIT extends MySqlIntegrationTestBase {
    private static final Instant AT = Instant.parse("2026-11-05T02:00:00Z");

    @Autowired
    TrendRuleService trends;

    @Autowired
    AlertStore alerts;

    @MockitoBean
    VitalSignApi vitalSigns;

    @MockitoSpyBean
    IncidentCatalogService catalog;

    @Autowired
    WeightLossProbe weightLosses;

    /** Nhận {@code WeightLossDetected} gửi cho 011 (listener phát trong luồng riêng nên không dùng ApplicationEvents). */
    static class WeightLossProbe {
        final List<WeightLossDetected> received = new CopyOnWriteArrayList<>();

        @EventListener
        void on(WeightLossDetected event) {
            received.add(event);
        }
    }

    @TestConfiguration
    static class ProbeConfig {
        @Bean
        WeightLossProbe weightLossProbe() {
            return new WeightLossProbe();
        }
    }

    @Test
    void FR_039a_weightDropOverThresholdCreatesTrendAlert() {
        long resident = TestUsers.nextId();
        long indicator = TestUsers.nextId();
        long latestId = TestUsers.nextId();
        weights(resident, indicator, latestId, "47.4", List.of(reading(1, resident, indicator, "50.0")));

        trends.onVitalSignConfirmed(confirmed(resident, indicator, latestId));

        assertThat(open(resident, TypeLock.forTrend("WEIGHT_LOSS")).level()).isEqualTo(IncidentLevel.MEDIUM);
        await().atMost(Duration.ofSeconds(10))
                .until(() -> weightLosses.received.stream().anyMatch(e -> e.residentId() == resident));
        var detected = weightLosses.received.stream()
                .filter(e -> e.residentId() == resident)
                .findFirst()
                .orElseThrow();
        assertThat(detected.highestRecordId()).isEqualTo(1);
        assertThat(detected.latestRecordId()).isEqualTo(latestId);
    }

    @Test
    void FR_039a_correctedWeightIsCheckedAgain() {
        long resident = TestUsers.nextId();
        long indicator = TestUsers.nextId();
        long recordId = TestUsers.nextId();
        weights(resident, indicator, recordId, "47.4", List.of(reading(1, resident, indicator, "50.0")));

        trends.onVitalSignCorrected(new VitalSignCorrected(
                recordId,
                resident,
                indicator,
                VitalSignLevel.NORMAL,
                VitalSignLevel.NORMAL,
                AT,
                "VITAL_SIGN_CORRECTED:" + recordId,
                AT.plusSeconds(3600)));

        assertThat(open(resident, TypeLock.forTrend("WEIGHT_LOSS")).level()).isEqualTo(IncidentLevel.MEDIUM);
    }

    @Test
    void FR_039a_smallDropOrNoPreviousWeighingCreatesNothing() {
        long resident = TestUsers.nextId();
        long indicator = TestUsers.nextId();
        long latestId = TestUsers.nextId();
        weights(resident, indicator, latestId, "48.5", List.of(reading(1, resident, indicator, "50.0")));
        trends.onVitalSignConfirmed(confirmed(resident, indicator, latestId));

        long other = TestUsers.nextId();
        weights(other, indicator, latestId, "40.0", List.of());
        trends.onVitalSignConfirmed(confirmed(other, indicator, latestId));

        none(resident, TypeLock.forTrend("WEIGHT_LOSS"));
        none(other, TypeLock.forTrend("WEIGHT_LOSS"));
    }

    @Test
    void FR_039b_missedMeasurementMergesByIndicatorAndMandatoryIsAtLeastMedium() {
        long resident = TestUsers.nextId();
        long indicator = TestUsers.nextId();
        String lock = TypeLock.forMissedMeasurement(indicator);

        trends.onMeasurementTaskMissed(missed(resident, indicator, Importance.MANDATORY, TestUsers.nextId()));
        var first = open(resident, lock);
        assertThat(first.level()).isEqualTo(IncidentLevel.MEDIUM);
        assertThat(first.occurrenceCount()).isEqualTo(1);

        trends.onMeasurementTaskMissed(missed(resident, indicator, Importance.MANDATORY, TestUsers.nextId()));
        await().atMost(Duration.ofSeconds(10)).until(() -> open(resident, lock).occurrenceCount() == 2);
        var merged = open(resident, lock);
        assertThat(merged.id()).isEqualTo(first.id());
        assertThat(merged.level()).isEqualTo(IncidentLevel.MEDIUM);
    }

    @Test
    void FR_039b_sameMissedTaskTwiceDoesNotCountTwice() {
        long resident = TestUsers.nextId();
        long indicator = TestUsers.nextId();
        long task = TestUsers.nextId();
        String lock = TypeLock.forMissedMeasurement(indicator);

        trends.onMeasurementTaskMissed(missed(resident, indicator, Importance.MANDATORY, task));
        open(resident, lock);
        trends.onMeasurementTaskMissed(missed(resident, indicator, Importance.MANDATORY, task));

        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(8))
                .until(() -> open(resident, lock).occurrenceCount() == 1);
    }

    @Test
    void FR_039_medicationRefusalMergesByMedicationWithRuleLevel() {
        long resident = TestUsers.nextId();
        long medication = TestUsers.nextId();
        long other = TestUsers.nextId();
        String lock = TypeLock.forMedication("TU_CHOI_LIEN_TIEP", medication);

        trends.onMedicationAlertRequested(refusal(resident, medication, AT));
        var first = open(resident, lock);
        assertThat(first.level()).isEqualTo(IncidentLevel.MEDIUM);

        trends.onMedicationAlertRequested(refusal(resident, medication, AT.plusSeconds(3600)));
        await().atMost(Duration.ofSeconds(10)).until(() -> open(resident, lock).occurrenceCount() == 2);
        assertThat(open(resident, lock).id()).isEqualTo(first.id());

        trends.onMedicationAlertRequested(refusal(resident, other, AT));
        assertThat(open(resident, TypeLock.forMedication("TU_CHOI_LIEN_TIEP", other))
                        .id())
                .isNotEqualTo(first.id());
    }

    @Test
    void FR_039_ruleLevelAppliesAndDisabledRuleCreatesNothing() {
        long resident = TestUsers.nextId();
        long medication = TestUsers.nextId();
        rule("MEDICATION_REFUSAL", true, IncidentLevel.CRITICAL);
        trends.onMedicationAlertRequested(refusal(resident, medication, AT));
        assertThat(open(resident, TypeLock.forMedication("TU_CHOI_LIEN_TIEP", medication))
                        .level())
                .isEqualTo(IncidentLevel.CRITICAL);

        long silent = TestUsers.nextId();
        rule("MEDICATION_REFUSAL", false, IncidentLevel.MEDIUM);
        trends.onMedicationAlertRequested(refusal(silent, medication, AT));
        none(silent, TypeLock.forMedication("TU_CHOI_LIEN_TIEP", medication));
    }

    /** Thay quy tắc đọc từ danh mục chỉ trong context của lớp test này (bản spy), không ghi danh mục dùng chung. */
    private void rule(String code, boolean enabled, IncidentLevel level) {
        var rules = new ArrayList<>(catalog.trendRules().stream()
                .filter(rule -> !rule.code().equals(code))
                .toList());
        rules.add(new TrendRuleView(code, code, enabled, level, "CFG-M05-04", 1));
        doReturn(rules).when(catalog).trendRules();
    }

    private static MedicationAlertRequestedEvent refusal(long resident, long medication, Instant at) {
        return new MedicationAlertRequestedEvent(
                "TU_CHOI_LIEN_TIEP",
                "TRUNG_BINH",
                resident,
                "dose:" + TestUsers.nextId(),
                "refusal:" + resident + ":" + medication,
                at,
                List.of());
    }

    /** Listener chạy bất đồng bộ sau commit: chờ cảnh báo xuất hiện. */
    private AlertRecord open(long resident, String lock) {
        await().atMost(Duration.ofSeconds(10))
                .until(() -> alerts.findOpenAlert(resident, lock).isPresent());
        return alerts.findOpenAlert(resident, lock).orElseThrow();
    }

    /** Không có cảnh báo trong 2 giây (listener đã kịp chạy). */
    private void none(long resident, String lock) {
        await().during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(8))
                .until(() -> alerts.findOpenAlert(resident, lock).isEmpty());
    }

    private void weights(
            long resident, long indicator, long latestId, String latest, List<VitalSignReadingView> before) {
        when(vitalSigns.latestPerType(resident))
                .thenReturn(Map.of(
                        indicator,
                        new LatestVitalSignView(
                                latestId,
                                indicator,
                                "CAN_NANG",
                                "Cân nặng",
                                "kg",
                                Map.of("value", new BigDecimal(latest)),
                                AT)));
        var current = reading(latestId, resident, indicator, latest);
        when(vitalSigns.withinRange(eq(resident), eq(indicator), any(), any())).thenAnswer(call -> {
            Instant to = call.getArgument(3);
            return to.equals(AT.plusMillis(1)) ? List.of(current) : new ArrayList<>(before);
        });
    }

    private static VitalSignReadingView reading(long id, long resident, long indicator, String value) {
        return new VitalSignReadingView(
                id,
                resident,
                indicator,
                Map.of("value", new BigDecimal(value)),
                VitalSignLevel.NORMAL,
                VitalSignConfirmationStatus.CONFIRMED,
                AT,
                Set.of());
    }

    private static VitalSignConfirmed confirmed(long resident, long indicator, long recordId) {
        return new VitalSignConfirmed(
                recordId, resident, indicator, VitalSignLevel.NORMAL, null, null, AT, Set.of(), "K" + recordId, AT);
    }

    private static MeasurementTaskMissed missed(long resident, long indicator, Importance importance, long task) {
        return new MeasurementTaskMissed(
                task,
                resident,
                TestUsers.nextId(),
                Set.of(indicator),
                "NOT_PERFORMED",
                importance,
                new CareEventContext("TASK", task, "MISSED", AT, Optional.empty(), List.of()));
    }
}
