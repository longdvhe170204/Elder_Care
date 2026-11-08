package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.internal.pricing.service.*;
import com.eldercare.backend.billing.internal.source.domain.CostSourceCandidate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SourceCostPricingTest {
    @Test
    void BR_M11_09_lastDayUsesRemainderAndPreservesContractBasis() {
        var prices = mock(CostPricingService.class);
        var date = LocalDate.of(2026, 11, 30);
        var basis = new CostPricingService.PriceBasis(CostPricingService.BasisKind.CONTRACT, 1L, 2, 3L, null);
        when(prices.event(42, 9, date, BigDecimal.ONE))
                .thenReturn(new CostPricingService.PriceQuote(
                        new BigDecimal("1000000"), new BigDecimal("1000000"), false, basis));
        var source = new CostSourceCandidate(
                42,
                "RESIDENCY_DAY",
                "42:" + date,
                "DAY",
                date,
                Instant.EPOCH,
                9L,
                CostType.RESIDENCY_FEE,
                "ngày",
                "Phí lưu trú",
                BigDecimal.ONE,
                new BigDecimal("100"),
                Set.of(),
                null,
                null,
                null,
                Map.of("billingPeriod", "MONTH"));
        var cost = new SourceCostPricing(prices).calculate(source);
        assertThat(cost.amount()).isEqualByComparingTo("33343");
        assertThat(cost.basis()).isEqualTo(basis);
        assertThat(cost.sourceValues()).containsEntry("billingPeriod", "MONTH");
    }
}
