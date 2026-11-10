package com.eldercare.backend.billing.internal.source.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.billing.internal.item.service.CostRecalculationService;
import com.eldercare.backend.billing.internal.period.service.CostLock;
import com.eldercare.backend.billing.internal.source.domain.IndexedBillingSource;
import com.eldercare.backend.billing.internal.source.domain.IndexedSourceMatch;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import com.eldercare.backend.care.api.events.ActivityCostSourceChanged;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.api.events.BillAdjustmentReason;
import com.eldercare.backend.medication.api.events.BilledSource;
import com.eldercare.backend.medication.api.events.MedicationBillAdjustedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 010 T021 (FR-020): đính chính thuốc của 006 và đổi nguồn hoạt động của 014 đi vào cùng luồng tính lại. */
class CostSourceChangeEventsTest {
    private final BillingSourceIndexStore index = mock(BillingSourceIndexStore.class);
    private final CostRecalculationService costs = mock(CostRecalculationService.class);
    private final ReminderMarkers markers = mock(ReminderMarkers.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final CostSourceChangeEvents events =
            new CostSourceChangeEvents(index, costs, mock(CostLock.class), markers, time);

    private static IndexedSourceMatch match(String type, String id, String unit) {
        return new IndexedSourceMatch(
                new IndexedBillingSource(
                        type,
                        id,
                        unit,
                        42,
                        LocalDate.of(2026, 10, 14),
                        Instant.EPOCH,
                        null,
                        77L,
                        BigDecimal.ONE,
                        "viên"),
                false,
                0,
                null);
    }

    @Test
    void FR_020_doseCorrectedToNotGiven_cancelsTheSource() {
        when(index.byTypeAndId("DOSE", "9")).thenReturn(List.of(match("DOSE", "9", "77")));

        events.medication(new MedicationBillAdjustedEvent(
                BilledSource.DOSE, 9, BigDecimal.ZERO, "viên", BillAdjustmentReason.CORRECTION));

        verify(index).change(eq("DOSE"), eq("9"), eq("77"), eq(null), eq(true), any());
        verify(costs).sourceChanged("DOSE", "9", "77", null, true, "M06.CORRECTION", null);
    }

    @Test
    void FR_020_returnedCarriedMedication_recalculatesWithNewQuantity() {
        when(index.byTypeAndId("DISPENSE_RECORD", "5")).thenReturn(List.of(match("DISPENSE_RECORD", "5", "77")));

        events.medication(new MedicationBillAdjustedEvent(
                BilledSource.DISPENSE_RECORD, 5, new BigDecimal("2.5"), "viên", BillAdjustmentReason.RETURNED));

        verify(costs).sourceChanged("DISPENSE_RECORD", "5", "77", new BigDecimal("2.5"), false, "M06.RETURNED", null);
    }

    @Test
    void FR_020_medicationAdjustment_withoutIndexedSource_isIgnored_notRetriedForever() {
        when(index.byTypeAndId("PRN_ADMINISTRATION", "3")).thenReturn(List.of());

        events.medication(new MedicationBillAdjustedEvent(
                BilledSource.PRN_ADMINISTRATION, 3, BigDecimal.ONE, "viên", BillAdjustmentReason.CORRECTION));

        verify(costs, never())
                .sourceChanged(anyString(), anyString(), anyString(), any(), eq(false), anyString(), any());
    }

    @Test
    void FR_031_activityCorrection_cancelsTheSource() {
        when(index.byKey("ACTIVITY_ATTENDANCE", "17", "OCCURRENCE"))
                .thenReturn(List.of(match("ACTIVITY_ATTENDANCE", "17", "OCCURRENCE")));
        when(markers.mark(anyString(), anyString(), eq(42L), anyString(), any()))
                .thenReturn(true);

        events.activity(new ActivityCostSourceChanged(
                "ACTIVITY_ATTENDANCE", "17", "OCCURRENCE", Optional.empty(), true, "CORRECTION:8", "C8"));

        verify(costs)
                .sourceChanged(
                        eq("ACTIVITY_ATTENDANCE"),
                        eq("17"),
                        eq("OCCURRENCE"),
                        eq(null),
                        eq(true),
                        eq("CORRECTION:8"),
                        anyString());
    }
}
