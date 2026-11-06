package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.billing.api.*;
import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.billing.internal.provision.domain.CostForBalancePort;
import com.eldercare.backend.billing.internal.purchase.domain.PurchaseBalancePort;
import com.eldercare.backend.support.fake.billing.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class CostSharedFakesTest {
    @Test
    void FR_023a_costFixtureReusesTheAdmissionOwnersRepresentativeFake() {
        try (var context = new AnnotationConfigApplicationContext(FakeM11Config.class)) {
            var source = context.getBean(com.eldercare.backend.admission.api.spi.RepresentativePort.class);
            assertThat(source).isInstanceOf(com.eldercare.backend.support.fake.admission.FakeRepresentatives.class);
            var fixture = (com.eldercare.backend.support.fake.admission.FakeRepresentatives) source;
            var day = LocalDate.of(2026, 11, 4);
            assertThat(source.isActiveRepresentative(10, 20, day)).isFalse();
            fixture.representative(20, 10);
            assertThat(source.isActiveRepresentative(10, 20, day)).isTrue();
            assertThat(source.isActiveRepresentative(11, 20, day)).isFalse();
            fixture.reset();
            assertThat(source.hasRepresentative(20)).isFalse();
        }
    }

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
