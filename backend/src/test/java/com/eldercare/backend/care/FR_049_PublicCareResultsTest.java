package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.care.api.PublicCareResultApi;
import com.eldercare.backend.care.api.dto.ResultType;
import com.eldercare.backend.care.internal.result.domain.CareResult;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import com.eldercare.backend.common.error.BusinessException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** T039: 012 tự kiểm đồng ý/nhật ký xem; 005 không đưa ghi chú nội bộ vào projection. */
class FR_049_PublicCareResultsTest {
    final CareResultStore store = mock(CareResultStore.class);
    final CorrectionApi corrections = mock(CorrectionApi.class);
    final Instant from = Instant.parse("2026-11-01T00:00:00Z"), to = from.plusSeconds(86400);

    PublicCareResultApi provider() throws Exception {
        return (PublicCareResultApi)
                Class.forName("com.eldercare.backend.care.internal.result.service.PublicCareResults")
                        .getConstructor(CareResultStore.class, CorrectionApi.class)
                        .newInstance(store, corrections);
    }

    void fixture() {
        when(store.history(1, from, to))
                .thenReturn(List.of(new CareResult(
                        2,
                        1L,
                        "ML",
                        Map.of(
                                "numericValue",
                                new BigDecimal("200"),
                                "note",
                                "Ghi chú nội bộ",
                                "upperLimitConfirmed",
                                true),
                        from,
                        from,
                        null,
                        3,
                        false,
                        false,
                        "ASSIGNED",
                        null)));
        when(corrections.current("M04.CARE_RESULT", List.of(2L))).thenReturn(Map.of());
    }

    @Test
    void FR_049_originalValuesExcludeInternalNotesAndConfirmationMetadata() throws Exception {
        fixture();
        assertThat(provider().results(1, from, to)).singleElement().satisfies(r -> {
            assertThat(r.resultType()).isEqualTo(ResultType.ML);
            assertThat(r.values()).containsOnlyKeys("numericValue");
        });
    }

    @Test
    void FR_049_correctedValuesReplaceTheOriginalResult() throws Exception {
        fixture();
        when(corrections.current("M04.CARE_RESULT", List.of(2L)))
                .thenReturn(Map.of(
                        2L,
                        new CurrentValue(
                                Map.of("numericValue", new BigDecimal("250"), "note", "Không gửi"), true, false)));
        assertThat(provider().results(1, from, to))
                .singleElement()
                .satisfies(r -> assertThat(r.values())
                        .containsEntry("numericValue", new BigDecimal("250"))
                        .doesNotContainKey("note"));
    }

    @Test
    void FR_049_voidedResultsAreNotPublishedToFamily() throws Exception {
        fixture();
        when(corrections.current("M04.CARE_RESULT", List.of(2L)))
                .thenReturn(Map.of(2L, new CurrentValue(null, true, true)));
        assertThat(provider().results(1, from, to)).isEmpty();
    }

    @Test
    void FR_052_summaryExplicitlyReportsTheDeferredBusinessFormula() throws Exception {
        var api = provider();
        assertThatThrownBy(() -> api.summary(1, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 2)))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("CARE_SOURCE_UNAVAILABLE"));
    }
}
