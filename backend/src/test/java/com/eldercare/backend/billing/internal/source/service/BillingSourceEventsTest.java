package com.eldercare.backend.billing.internal.source.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.eldercare.backend.billing.internal.source.domain.IndexedBillingSource;
import com.eldercare.backend.care.api.events.ActivityCostSourceBilled;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * FR-005 (010), FR-032 (014): nguồn tính phí của hoạt động đến 010 qua sự kiện của care (đổi từ
 * {@code billing.api.events.CostSourceBilled} để gỡ vòng billing ↔ care, 2026-11-07), ghi chỉ mục như cũ.
 */
class BillingSourceEventsTest {

    @Test
    void FR_005_activityCostSource_isIndexedLikeTheFormerBillingEvent() {
        BillingSourceIndex index = mock(BillingSourceIndex.class);
        Instant at = Instant.parse("2026-11-07T02:00:00Z");

        new BillingSourceEvents(index)
                .onActivityCostSourceBilled(new ActivityCostSourceBilled(
                        "ACTIVITY_ATTENDANCE",
                        "17",
                        "OCCURRENCE",
                        42,
                        Optional.of(5L),
                        BigDecimal.ONE,
                        LocalDate.of(2026, 11, 7),
                        at,
                        "TRIP_DEPARTURE:17"));

        verify(index)
                .record(
                        new IndexedBillingSource(
                                "ACTIVITY_ATTENDANCE",
                                "17",
                                "OCCURRENCE",
                                42,
                                LocalDate.of(2026, 11, 7),
                                at,
                                5L,
                                null,
                                BigDecimal.ONE),
                        "TRIP_DEPARTURE:17");
    }
}
