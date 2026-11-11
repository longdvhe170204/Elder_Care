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
    void FR_009_lastScheduledSession_takesRemainder() {
        var prices = mock(CostPricingService.class);
        when(prices.event(eq(42L), eq(7L), any(), eq(BigDecimal.ONE))).thenReturn(quote("1000000"));
        var facts = Map.<String, Object>of(
                "billingPeriod", "MONTH", "scheduledSessionCount", 3, "lastScheduledSession", true);

        assertThat(new SourceCostPricing(prices)
                        .calculate(session(7L, "100", facts))
                        .amount())
                .isEqualByComparingTo("333334");
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

    @Test
    void FR_012_unresolvedService_isMissingUnitPrice_withoutAmount() {
        var prices = mock(CostPricingService.class);
        var date = LocalDate.of(2026, 10, 14);
        var source = new CostSourceCandidate(
                42,
                "OVERNIGHT_STAY",
                "S1",
                "NIGHT",
                date,
                Instant.EPOCH,
                null,
                CostType.FAMILY_OVERNIGHT_STAY,
                "đêm",
                "Người thân ở lại",
                BigDecimal.ONE,
                null,
                Set.of(),
                null,
                null,
                null,
                Map.of());

        var cost = new SourceCostPricing(prices).calculate(source);

        assertThat(cost.amount()).isNull();
        assertThat(cost.flags()).contains("MISSING_UNIT_PRICE");
        verifyNoInteractions(prices);
    }
}
