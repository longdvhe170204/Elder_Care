package com.cambae.backend.m11;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.m11.api.*;
import com.cambae.backend.m11.api.dto.*;
import com.cambae.backend.m11.provision.internal.domain.CostForBalancePort;
import com.cambae.backend.m11.purchase.internal.domain.PurchaseBalancePort;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.support.fake.m11.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class CostSharedFakesTest {
    @Test
    void FR_009_sharedPriceFakeDistinguishesZeroFromUnknownAndKeepsSignedTotal() {
        var fake = new FakeCostForBalance();
        var day = LocalDate.of(2026, 10, 29);
        fake.total(23, -500).dailyFee(23, day, 0);
        assertThat(fake.unfinalizedTotal(23)).isEqualTo(-500);
        assertThat(fake.baseDailyFee(23, day)).contains(0L);
        assertThat(fake.baseDailyFee(23, day.plusDays(1))).isEmpty();
        fake.reset();
        assertThat(fake.unfinalizedTotal(23)).isZero();
        assertThat(fake.baseDailyFee(23, day)).isEmpty();
    }

    @Test
    void FR_024_sharedBalanceFakeKeepsTheAmountAndInstantInItsQuery() {
        var fake = new FakePurchaseBalance();
        var at = Instant.parse("2026-10-29T10:00:00Z");
        fake.insufficient(23, 500, at, true);
        assertThat(fake.insufficientBalance(23, 500, at)).isTrue();
        assertThat(fake.insufficientBalance(23, 501, at)).isFalse();
        assertThat(fake.insufficientBalance(23, 500, at.plusSeconds(1))).isFalse();
        fake.reset();
        assertThat(fake.insufficientBalance(23, 500, at)).isFalse();
    }

    @Test
    void FR_034a_finalPeriodFakeNeverInventsASatisfiedCondition() {
        var fake = new FakeFinalPeriodCosts();
        assertThatIllegalStateException().isThrownBy(() -> fake.finalPeriodCondition(23));
        var unmet = new FinalPeriodConditionView(false, List.of("Bảng kỳ cuối chưa chốt"));
        fake.condition(23, unmet);
        assertThat(fake.finalPeriodCondition(23)).isEqualTo(unmet);
        fake.reset();
        assertThatIllegalStateException().isThrownBy(() -> fake.finalPeriodCondition(23));
    }

    @Test
    void FR_042_statisticsFakeSeparatesConfiguredScopesAndIncludesBothDateBounds() {
        var day = LocalDate.of(2026, 10, 29);
        var scope = new ScopeFilter(false, Set.of(1L), Set.of(), Set.of());
        var before = row(1, day.minusDays(1));
        var first = row(2, day);
        var last = row(3, day.plusDays(1));
        var after = row(4, day.plusDays(2));
        var fake = new FakeCostStatistics().items(scope, List.of(before, first, last, after));
        assertThat(fake.items(scope, day, day.plusDays(1))).containsExactly(first, last);
        assertThat(fake.items(ScopeFilter.FACILITY, day, day.plusDays(1))).isEmpty();
        fake.reset();
        assertThat(fake.items(scope, day, day.plusDays(1))).isEmpty();
    }

    private static CostItemReportView row(long id, LocalDate day) {
        return new CostItemReportView(
                id,
                23,
                CostType.OTHER,
                "MANUAL",
                31,
                null,
                null,
                day,
                BigDecimal.ONE,
                500L,
                500L,
                Set.of(),
                "DRAFT",
                null,
                null);
    }

    @Test
    void FR_042_oneOwnerProvidesAllSharedCostFakesOnlyInTestConfiguration() {
        try (var context = new AnnotationConfigApplicationContext(FakeM11Config.class)) {
            assertThat(context.getBean(FamilyPortalCostApi.class)).isInstanceOf(FakeFamilyPortalCosts.class);
            assertThat(context.getBean(FinalPeriodCostApi.class)).isInstanceOf(FakeFinalPeriodCosts.class);
            assertThat(context.getBean(CostStatisticsApi.class)).isInstanceOf(FakeCostStatistics.class);
            assertThat(context.getBean(CostForBalancePort.class)).isInstanceOf(FakeCostForBalance.class);
            assertThat(context.getBean(PurchaseBalancePort.class)).isInstanceOf(FakePurchaseBalance.class);
        }
    }
}
