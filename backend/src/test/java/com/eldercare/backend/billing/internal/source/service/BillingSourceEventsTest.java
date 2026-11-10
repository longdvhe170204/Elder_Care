package com.eldercare.backend.billing.internal.source.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.eldercare.backend.billing.internal.source.domain.IndexedBillingSource;
import com.eldercare.backend.care.api.dto.AttendanceStatus;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.ActivityCostSourceBilled;
import com.eldercare.backend.care.api.events.AttendanceDetermined;
import com.eldercare.backend.medication.api.events.BilledSource;
import com.eldercare.backend.medication.api.events.MedicationBilledEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

    @Test
    void T011_medicationSource_keepsUsageUnitForDisplay() {
        BillingSourceIndex index = mock(BillingSourceIndex.class);
        Instant at = Instant.parse("2026-10-14T01:00:00Z");

        new BillingSourceEvents(index)
                .onMedicationBilled(
                        new MedicationBilledEvent(BilledSource.DOSE, 9, 42, 77, new BigDecimal("0.5"), "viên", at));

        verify(index)
                .record(
                        new IndexedBillingSource(
                                "DOSE",
                                "9",
                                "77",
                                42,
                                LocalDate.of(2026, 10, 14),
                                at,
                                null,
                                77L,
                                new BigDecimal("0.5"),
                                "viên"),
                        "M06.BILLED:DOSE:9");
    }

    private static CareEventContext context() {
        return new CareEventContext(
                "M04.DAY_CARE_ATTENDANCE",
                31,
                "CHECK_IN:0",
                Instant.parse("2026-10-14T00:30:00Z"),
                Optional.empty(),
                List.of());
    }

    @Test
    void FR_005b_adHocSessionPresent_isIndexedAsOneSession() {
        BillingSourceIndex index = mock(BillingSourceIndex.class);
        var day = LocalDate.of(2026, 10, 14);

        new BillingSourceEvents(index)
                .onAttendanceDetermined(new AttendanceDetermined(42, day, AttendanceStatus.PRESENT, true, context()));

        verify(index)
                .record(
                        new IndexedBillingSource(
                                "DAY_CARE_SESSION",
                                "42:2026-10-14:AD_HOC",
                                "SESSION",
                                42,
                                day,
                                Instant.parse("2026-10-14T00:30:00Z"),
                                null,
                                null,
                                BigDecimal.ONE),
                        "M04.AD_HOC:M04.DAY_CARE_ATTENDANCE:31");
    }

    @Test
    void FR_005b_scheduledSession_isNotIndexed_becauseItComesFrom004() {
        BillingSourceIndex index = mock(BillingSourceIndex.class);

        new BillingSourceEvents(index)
                .onAttendanceDetermined(new AttendanceDetermined(
                        42, LocalDate.of(2026, 10, 14), AttendanceStatus.PRESENT, false, context()));

        verify(index, never()).record(any(), anyString());
    }
}
