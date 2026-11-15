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

    private static CostSourceCandidate session(Long serviceId, String factor, Map<String, Object> facts) {
        var date = LocalDate.of(2026, 10, 14);
        return new CostSourceCandidate(
                42,
                "DAY_CARE_SESSION",
                "42:" + date + ":1",
                "SESSION",
                date,
                Instant.EPOCH,
                serviceId,
                CostType.DAY_CARE_SESSION_FEE,
                "buổi",
                "Phí buổi bán trú",
                BigDecimal.ONE,
                new BigDecimal(factor),
                Set.of(),
                null,
                null,
                null,
                facts);
    }

    private static CostPricingService.PriceQuote quote(String price) {
        return new CostPricingService.PriceQuote(
                new BigDecimal(price),
                new BigDecimal(price),
                false,
                new CostPricingService.PriceBasis(CostPricingService.BasisKind.CONTRACT, 1L, 1, null, null));
    }

    @Test
    void FR_009_dayCareMonthlyPrice_isSplitOverScheduledSessions_timesAttendanceFactor() {
        var prices = mock(CostPricingService.class);
        when(prices.event(eq(42L), eq(7L), any(), eq(BigDecimal.ONE))).thenReturn(quote("3100000"));
        var facts = Map.<String, Object>of(
                "billingPeriod", "MONTH", "scheduledSessionCount", 20, "lastScheduledSession", false);

        assertThat(new SourceCostPricing(prices)
                        .calculate(session(7L, "100", facts))
                        .amount())
                .isEqualByComparingTo("155000");
        assertThat(new SourceCostPricing(prices)
                        .calculate(session(7L, "50", facts))
                        .amount())
                .isEqualByComparingTo("77500");
    }

    @Test
    void FR_009_sessionPrice_timesAttendanceFactor() {
        var prices = mock(CostPricingService.class);
        when(prices.event(eq(42L), eq(7L), any(), eq(BigDecimal.ONE))).thenReturn(quote("200000"));

        var cost = new SourceCostPricing(prices).calculate(session(7L, "50", Map.of("billingPeriod", "SESSION")));

        assertThat(cost.amount()).isEqualByComparingTo("100000");
        assertThat(cost.unitPrice()).isEqualByComparingTo("200000");
    }

    @Test
    void FR_005b_adHocSession_usesPriceVersion_notMonthlyContractPrice() {
        var prices = mock(CostPricingService.class);
        when(prices.catalog(eq(7L), any(), eq(BigDecimal.ONE))).thenReturn(quote("180000"));

        var cost = new SourceCostPricing(prices)
                .calculate(session(7L, "100", Map.of("billingPeriod", "SESSION", "adHocSession", true)));

        assertThat(cost.amount()).isEqualByComparingTo("180000");
        verify(prices, never()).event(anyLong(), anyLong(), any(), any());
    }
}
