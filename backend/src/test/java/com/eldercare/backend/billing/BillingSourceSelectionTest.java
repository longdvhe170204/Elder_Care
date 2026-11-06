package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.eldercare.backend.billing.internal.source.domain.IndexedBillingSource;
import com.eldercare.backend.billing.internal.source.domain.IndexedSourceMatch;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import com.eldercare.backend.billing.internal.source.service.BillingSourceSelection;
import com.eldercare.backend.billing.internal.source.service.BillingSourceSelection.SourceUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Quyết định 06/11: không chọn ngầm mọi đơn vị khi chỉ có id mơ hồ. */
class BillingSourceSelectionTest {
    private final BillingSourceIndexStore store = mock(BillingSourceIndexStore.class);
    private final BillingSourceSelection selection = new BillingSourceSelection(store);

    private IndexedSourceMatch source(String id, String unit) {
        return source("OVERNIGHT_STAY", id, unit);
    }

    private IndexedSourceMatch source(String type, String id, String unit) {
        return new IndexedSourceMatch(
                new IndexedBillingSource(
                        type,
                        id,
                        unit,
                        42,
                        LocalDate.of(2026, 11, 4),
                        Instant.parse("2026-11-04T01:00:00Z"),
                        null,
                        null,
                        BigDecimal.ONE),
                false,
                0,
                null);
    }

    @Test
    void FR_005_exactUnitSelectsOnlyTheRequestedNightOfAStay() {
        var first = source("stay-17", "2026-11-03");
        var second = source("stay-17", "2026-11-04");
        when(store.findBySourceIds(42, "OVERNIGHT_STAY", List.of("stay-17"))).thenReturn(List.of(first, second));

        var result = selection.byUnits(42, "OVERNIGHT_STAY", List.of(new SourceUnit("stay-17", "2026-11-04")));

        assertThat(result.selected()).containsExactly(second);
        assertThat(result.skipped()).isEmpty();
    }

    @Test
    void DBR_15_twoMedicationUnitsInTheSameSourceRemainSeparateSelections() {
        var first = source("DISPENSE_RECORD", "handover-7", "101");
        var second = source("DISPENSE_RECORD", "handover-7", "102");
        when(store.findBySourceIds(42, "DISPENSE_RECORD", List.of("handover-7")))
                .thenReturn(List.of(first, second));

        var result = selection.byUnits(
                42,
                "DISPENSE_RECORD",
                List.of(new SourceUnit("handover-7", "102"), new SourceUnit("handover-7", "101")));

        assertThat(result.selected()).containsExactly(second, first);
        assertThat(result.skipped()).isEmpty();
    }

    @Test
    void FR_005_legacyIdStillSelectsItsOnlyBillingUnit() {
        var only = source("stay-17", "2026-11-04");
        when(store.findBySourceIds(42, "OVERNIGHT_STAY", List.of("stay-17"))).thenReturn(List.of(only));

        var result = selection.byIds(42, "OVERNIGHT_STAY", List.of("stay-17"));

        assertThat(result.selected()).containsExactly(only);
        assertThat(result.skipped()).isEmpty();
    }

    @Test
    void FR_005_ambiguousLegacyIdIsSkippedWithoutSelectingAnyUnit() {
        when(store.findBySourceIds(42, "OVERNIGHT_STAY", List.of("stay-17")))
                .thenReturn(List.of(source("stay-17", "first"), source("stay-17", "second")));

        var result = selection.byIds(42, "OVERNIGHT_STAY", List.of("stay-17"));

        assertThat(result.selected()).isEmpty();
        assertThat(result.skipped()).singleElement().satisfies(skipped -> {
            assertThat(skipped.errorCode()).isEqualTo("CONDITION_NOT_MET");
            assertThat(skipped.source()).isEqualTo(new SourceUnit("stay-17", null));
            assertThat(skipped.reason()).isNotBlank();
        });
    }

    @Test
    void FR_005_aMissingUnitIsSkippedWithoutFallingBackToAnotherUnit() {
        when(store.findBySourceIds(42, "OVERNIGHT_STAY", List.of("stay-17")))
                .thenReturn(List.of(source("stay-17", "first")));

        var result = selection.byUnits(42, "OVERNIGHT_STAY", List.of(new SourceUnit("stay-17", "missing")));

        assertThat(result.selected()).isEmpty();
        assertThat(result.skipped()).singleElement().satisfies(skipped -> {
            assertThat(skipped.errorCode()).isEqualTo("NOT_A_BILLABLE_EVENT");
            assertThat(skipped.source()).isEqualTo(new SourceUnit("stay-17", "missing"));
        });
    }

    @Test
    void FR_005_aMixedBatchKeepsTheUnambiguousSourceAndReportsEverySkippedSource() {
        var only = source("single", "night");
        var ids = List.of("single", "multiple", "missing");
        when(store.findBySourceIds(42, "OVERNIGHT_STAY", ids))
                .thenReturn(List.of(source("multiple", "first"), only, source("multiple", "second")));

        var result = selection.byIds(42, "OVERNIGHT_STAY", ids);

        assertThat(result.selected()).containsExactly(only);
        assertThat(result.skipped())
                .extracting(skipped -> skipped.source().sourceId())
                .containsExactly("multiple", "missing");
        assertThat(result.skipped())
                .extracting(skipped -> skipped.errorCode())
                .containsExactly("CONDITION_NOT_MET", "NOT_A_BILLABLE_EVENT");
        verify(store).findBySourceIds(42, "OVERNIGHT_STAY", ids);
        verifyNoMoreInteractions(store);
    }

    @Test
    void FR_006_selectionRetainsCurrentCancellationVersionAndExistingCostForTheCommandToRecheck() {
        var original = source("stay-17", "night");
        var current = new IndexedSourceMatch(original.source(), true, 7, 123L);
        when(store.findBySourceIds(42, "OVERNIGHT_STAY", List.of("stay-17"))).thenReturn(List.of(current));

        var result = selection.byIds(42, "OVERNIGHT_STAY", List.of("stay-17"));

        assertThat(result.selected()).containsExactly(current);
        assertThat(result.selected().getFirst().existingItemId()).isEqualTo(123L);
    }
}
