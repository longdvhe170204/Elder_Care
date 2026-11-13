package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.dto.ResidencyDayView;
import com.eldercare.backend.billing.internal.source.domain.BillingSourceKey;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import com.eldercare.backend.billing.internal.source.service.ResidencyBillingSources;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** T011: chỉ đọc ngày lưu trú từ API nguồn, giữ đầy đủ căn cứ cho lệnh lập khoản. */
class ResidencyBillingSourcesTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 4);
    private final ResidencyDayApi residency = mock(ResidencyDayApi.class);
    private final BillingSourceIndexStore index = mock(BillingSourceIndexStore.class);
    private final ResidencyBillingSources service = new ResidencyBillingSources(residency, index);

    private ResidencyDayView day(LocalDate date, boolean released, boolean notAdmitted, boolean outside) {
        return new ResidencyDayView(date, 11L, 21L, "ABSENT", 31L, released, notAdmitted, true, outside);
    }

    @Test
    void FR_005a_lookupKeepsContractAbsenceAndNotYetAdmittedFactsWithoutWritingTheIndex() {
        var original = day(DAY, false, true, false);
        var key = new BillingSourceKey("42:" + DAY, "DAY");
        when(residency.daysBetween(42, DAY, DAY)).thenReturn(List.of(original));
        when(index.effectiveItems(42, "RESIDENCY_DAY", List.of(key.sourceId()))).thenReturn(Map.of());

        assertThat(service.between(42, DAY, DAY)).singleElement().satisfies(source -> {
            assertThat(source.residentId()).isEqualTo(42);
            assertThat(source.key()).isEqualTo(key);
            assertThat(source.day()).isEqualTo(original);
            assertThat(source.existingItemId()).isNull();
        });
        verify(index).effectiveItems(42, "RESIDENCY_DAY", List.of(key.sourceId()));
        verifyNoMoreInteractions(index);
    }

    @Test
    void FR_005_sourcesWithExistingCostsRemainVisibleAndKeepTheirEffectiveCostId() {
        var first = day(DAY, false, false, false);
        var second = day(DAY.plusDays(1), false, false, false);
        var key = new BillingSourceKey("42:" + DAY, "DAY");
        when(residency.daysBetween(42, DAY, DAY.plusDays(1))).thenReturn(List.of(second, first));
        when(index.effectiveItems(42, "RESIDENCY_DAY", List.of("42:" + DAY, "42:" + DAY.plusDays(1))))
                .thenReturn(Map.of(key, 91L));

        var sources = service.between(42, DAY, DAY.plusDays(1));

        assertThat(sources).extracting(source -> source.day().date()).containsExactly(DAY, DAY.plusDays(1));
        assertThat(sources.getFirst().existingItemId()).isEqualTo(91L);
        assertThat(sources.getLast().existingItemId()).isNull();
    }

    @Test
    void FR_005a_aSecondLookupUsesTheCurrentSourceInsteadOfThePreviousSnapshot() {
        var before = day(DAY, false, false, false);
        var after = day(DAY, true, false, false);
        when(residency.daysBetween(42, DAY, DAY)).thenReturn(List.of(before), List.of(after));
        when(index.effectiveItems(42, "RESIDENCY_DAY", List.of("42:" + DAY))).thenReturn(Map.of());

        assertThat(service.between(42, DAY, DAY).getFirst().day().bedReleased()).isFalse();
        assertThat(service.between(42, DAY, DAY).getFirst().day().bedReleased()).isTrue();
        verify(residency, times(2)).daysBetween(42, DAY, DAY);
    }
}
