package com.eldercare.backend.health.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignComponent;
import com.eldercare.backend.health.internal.measurement.domain.VitalSignRecord;
import com.eldercare.backend.health.internal.measurement.service.VitalSignCorrectionProvider;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** BR-M06-06 (US10): lịch sử chỉ số dùng giá trị hiện hành sau đính chính, giữ giá trị gốc; bản hủy ghi nhận ẩn. */
class VitalSignHistoryCorrectionTest {
    private static final Instant AT = Instant.parse("2026-11-02T08:00:00Z");

    private VitalSignMeasurementService measurements;
    private CorrectionApi corrections;
    private VitalSignCorrectionProvider provider;
    private VitalSignController controller;

    @BeforeEach
    void setUp() {
        measurements = mock(VitalSignMeasurementService.class);
        corrections = mock(CorrectionApi.class);
        provider = mock(VitalSignCorrectionProvider.class);
        var accounts = mock(AccountApi.class);
        when(accounts.fullName(anyLong())).thenReturn(Optional.of("Điều dưỡng A"));
        controller = new VitalSignController(measurements, mock(CurrentUser.class), accounts, corrections, provider);
    }

    private static VitalSignRecord record(long id, String value, VitalSignLevel level) {
        return new VitalSignRecord(
                id,
                10L,
                20L,
                AT,
                level,
                VitalSignConfirmationStatus.CONFIRMED,
                Map.of("SYS", new VitalSignComponent(new BigDecimal(value), level)),
                Set.of(),
                AT);
    }

    @Test
    void BR_M06_06_correctedRecordShowsCurrentValueAndKeepsOriginal() {
        var original = record(1, "180", VitalSignLevel.DANGEROUS);
        when(measurements.getHistory(VitalSignMeasurementService.target(10L), 10L, null, null, null))
                .thenReturn(List.of(original));
        when(corrections.current(eq("M06.VITAL_SIGN_RECORD"), any()))
                .thenReturn(Map.of(1L, new CurrentValue(Map.of("values", Map.of("SYS", "130")), true, false)));
        when(provider.levelOf(eq(original), any())).thenReturn(VitalSignLevel.NORMAL);

        var history = controller.getVitalSigns(10L, null, null, null);

        assertThat(history).singleElement().satisfies(item -> {
            assertThat(item.values()).containsEntry("SYS", new BigDecimal("130"));
            assertThat(item.originalValues()).containsEntry("SYS", new BigDecimal("180"));
            assertThat(item.level()).isEqualTo(VitalSignLevel.NORMAL);
        });
    }

    @Test
    void BR_M06_06_uncorrectedRecordIsUnchangedAndVoidedRecordIsHidden() {
        var plain = record(1, "120", VitalSignLevel.NORMAL);
        var voided = record(2, "125", VitalSignLevel.NORMAL);
        when(measurements.getHistory(VitalSignMeasurementService.target(10L), 10L, null, null, null))
                .thenReturn(List.of(plain, voided));
        when(corrections.current(eq("M06.VITAL_SIGN_RECORD"), any()))
                .thenReturn(Map.of(2L, new CurrentValue(null, true, true)));

        var history = controller.getVitalSigns(10L, null, null, null);

        assertThat(history).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(1L);
            assertThat(item.originalValues()).isNull();
        });
    }
}
