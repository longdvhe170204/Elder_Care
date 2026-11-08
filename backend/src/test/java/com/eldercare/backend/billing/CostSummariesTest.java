package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.billing.internal.provision.domain.ProvisionalItem;
import com.eldercare.backend.billing.internal.provision.persistence.CostProvisionStore;
import com.eldercare.backend.billing.internal.provision.service.CostSummaries;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CostSummariesTest {
    @Test
    void FR_038_038a_dateBoundsAreInclusiveAndUnfinalizedTotalIncludesPriorPeriodsAndSignedAdjustments() {
        var store = mock(CostProvisionStore.class);
        var date = LocalDate.of(2026, 11, 1);
        when(store.provisional(7))
                .thenReturn(List.of(
                        new ProvisionalItem(CostType.SERVICE, "EVENT_BASED", "DRAFT", 100L, date),
                        new ProvisionalItem(CostType.SERVICE, "ADJUSTMENT", "CHECKED", -30L, date.plusDays(7)),
                        new ProvisionalItem(CostType.OTHER, "MANUAL", "CHECKED", 1000L, date),
                        new ProvisionalItem(
                                CostType.PURCHASE_ASSISTANCE, "PURCHASE_ASSISTANCE", "APPROVED", 200L, date),
                        new ProvisionalItem(CostType.SERVICE, "EVENT_BASED", "APPROVED", 50L, date.minusDays(1)),
                        new ProvisionalItem(CostType.SERVICE, "EVENT_BASED", "DRAFT", null, date),
                        new ProvisionalItem(CostType.SERVICE, "EVENT_BASED", "CANCELLED", 900L, date)));
        var summaries = new CostSummaries(store);
        var current = summaries.provisional(7, date, date.plusDays(7));
        assertThat(current.totalsByType())
                .containsExactly(
                        new CostTypeTotal(CostType.SERVICE, 70), new CostTypeTotal(CostType.PURCHASE_ASSISTANCE, 200));
        assertThat(current.hasUnpricedItems()).isTrue();
        assertThat(summaries.provisional(7, null, null).totalsByType())
                .contains(new CostTypeTotal(CostType.SERVICE, 120));
        assertThat(summaries.provisional(7, date.plusDays(8), date.plusDays(9)).hasUnpricedItems())
                .isFalse();
    }
}
