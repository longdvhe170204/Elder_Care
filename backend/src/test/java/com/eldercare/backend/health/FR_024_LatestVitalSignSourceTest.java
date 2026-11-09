package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.health.api.VitalSignApi;
import com.eldercare.backend.health.api.dto.LatestVitalSignView;
import com.eldercare.backend.health.internal.measurement.service.DefaultLatestVitalSignSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** 001 FR-024: chỉ số gần nhất cho chi tiết đánh giá, chuyển tiếp từ {@code VitalSignApi.latestPerType} (giá trị hiện hành). */
class FR_024_LatestVitalSignSourceTest {
    @Test
    void FR_024_forwardsCurrentValuesWithOriginalMeasuredTime() {
        var api = mock(VitalSignApi.class);
        var at = Instant.parse("2026-11-08T01:00:00Z");
        when(api.latestPerType(7L))
                .thenReturn(Map.of(
                        3L,
                        new LatestVitalSignView(
                                100L,
                                3L,
                                "SPO2",
                                "Độ bão hòa oxy ngoại vi",
                                "%",
                                Map.of("VALUE", new BigDecimal("96")),
                                at)));

        var latest = new DefaultLatestVitalSignSource(api).latest(7L);

        assertThat(latest).hasSize(1);
        var item = latest.get(0);
        assertThat(item.indicatorId()).isEqualTo(3L);
        assertThat(item.indicatorCode()).isEqualTo("SPO2");
        assertThat(item.unit()).isEqualTo("%");
        assertThat(item.values()).containsEntry("VALUE", new BigDecimal("96"));
        assertThat(item.measuredAt()).isEqualTo(at);
        when(api.latestPerType(8L)).thenReturn(Map.of());
        assertThat(new DefaultLatestVitalSignSource(api).latest(8L)).isEmpty();
    }
}
