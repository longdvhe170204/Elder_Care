package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.billing.api.*;
import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.support.fake.billing.*;
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
}
