package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.administration.correction.api.dto.CurrentValue;
import com.eldercare.backend.care.api.PublicCareResultApi;
import com.eldercare.backend.care.api.dto.ResultType;
import com.eldercare.backend.care.internal.result.domain.CareResult;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
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

    CareResult row(long id, String type, String ml, String at) {
        return new CareResult(
                id,
                1L,
                type,
                Map.of("numericValue", new BigDecimal(ml)),
                Instant.parse(at),
                Instant.parse(at),
                null,
                3,
                false,
                false,
                "ASSIGNED",
                null);
    }

    /**
     * Chủ dự án chốt 2026-11-12: lượng nước trung bình = tổng lượng nước từng ngày chia số ngày có ít nhất một lần ghi;
     * ngày không ghi không tính; trả kèm số ngày có dữ liệu. Bản đã Hủy ghi nhận bỏ, bản đính chính lấy giá trị mới.
     */
    @Test
    void FR_052_averageWaterIsPerDayWithRecordsOnly() throws Exception {
        var dayFrom = LocalDate.of(2026, 11, 1);
        var dayTo = LocalDate.of(2026, 11, 3);
        var start = dayFrom.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        var end = dayTo.plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        when(store.history(1, start, end))
                .thenReturn(List.of(
                        // ngày 01/11 giờ Việt Nam: 200 + 300
                        row(10, "ML", "200", "2026-10-31T18:00:00Z"),
                        row(11, "ML", "300", "2026-11-01T10:00:00Z"),
                        // ngày 02/11 không ghi nước, chỉ có kết quả khác
                        row(12, "PERFORMED", "1", "2026-11-02T03:00:00Z"),
                        // ngày 03/11: 100 (đính chính thành 400) và 999 đã Hủy ghi nhận
                        row(13, "ML", "100", "2026-11-03T02:00:00Z"),
                        row(14, "ML", "999", "2026-11-03T03:00:00Z")));
        when(corrections.current("M04.CARE_RESULT", List.of(10L, 11L, 12L, 13L, 14L)))
                .thenReturn(Map.of(
                        13L, new CurrentValue(Map.of("numericValue", new BigDecimal("400")), true, false),
                        14L, new CurrentValue(null, false, true)));

        var summary = provider().summary(1, dayFrom, dayTo);

        assertThat(summary.averageWaterMl())
                .hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("450"));
        assertThat(summary.waterDays()).isEqualTo(2);
    }

    @Test
    void FR_052_noWaterRecordsGivesEmptyAverageAndZeroDays() throws Exception {
        var day = LocalDate.of(2026, 11, 1);
        when(store.history(eq(1L), any(), any())).thenReturn(List.of());
        when(corrections.current(eq("M04.CARE_RESULT"), anyCollection())).thenReturn(Map.of());

        var summary = provider().summary(1, day, day);

        assertThat(summary.averageWaterMl()).isEmpty();
        assertThat(summary.waterDays()).isZero();
    }
}
