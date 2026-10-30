package com.eldercare.backend.support.fake.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.CarePlanApi;
import com.eldercare.backend.care.api.ShiftTaskApi;
import com.eldercare.backend.care.api.TaskTypeApi;
import com.eldercare.backend.care.api.dto.HandoverTransferResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.EnableTransactionManagement;

class T003_MergedApiCompatibilityTest {
    @Test
    void T003_bothFakeConfigurationsResolveApisWithoutAmbiguousPrimaryBeans() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=test")
                .withUserConfiguration(
                        FakeM04Config.class,
                        FakeM04ApiConfig.class,
                        com.eldercare.backend.support.fake.health.FakeVitalSigns.Config.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CarePlanApi.class)).isNotNull();
                    assertThat(context.getBean(TaskTypeApi.class)).isNotNull();
                    assertThat(context.getBean(ShiftTaskApi.class)).isNotNull();
                    assertThat(context.getBean(FakeCareRequirements.class)).isNotNull();
                    assertThat(context.getBean(FakeCareShifts.class)).isNotNull();
                });
    }

    @Test
    void T003_fixtureSetupNeedsNoTransactionButBusinessCallbacksStillRequireOne() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=test")
                .withUserConfiguration(FakeM04Config.class, Transactions.class)
                .run(context -> {
                    var fake = context.getBean(FakeCareRequirements.class);
                    assertThatCode(() -> {
                                fake.failOnEnd(false);
                                fake.onResign(() -> {});
                                fake.onAbsent((staff, shift) -> {});
                                fake.plan(1, Set.of(2L));
                            })
                            .doesNotThrowAnyException();
                    assertThatThrownBy(() -> fake.onAbsence(1, 2)).isInstanceOf(IllegalTransactionStateException.class);
                    assertThatThrownBy(() -> fake.onAssignmentEnded(1, 2))
                            .isInstanceOf(IllegalTransactionStateException.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class Transactions {
        @Bean
        DataSourceTransactionManager transactionManager() {
            return new DataSourceTransactionManager(new DriverManagerDataSource());
        }
    }

    @Test
    void FR_047_handoverFixtureAcceptsMissingNextShiftAndMatchesAllArguments() {
        var fixture = new FakeM04ApiFixture();
        var at = Instant.parse("2026-10-28T00:00:00Z");
        var result = new HandoverTransferResult(List.of(4L), List.of());
        fixture.reply(
                ShiftTaskApi.class, "onHandoverConfirmed", Arrays.asList(1L, 2L, null, Set.of(4L), at), () -> result);
        var api = fixture.api(ShiftTaskApi.class);
        assertThat(api.onHandoverConfirmed(1L, 2L, null, Set.of(4L), at)).isEqualTo(result);
        assertThatThrownBy(() -> api.onHandoverConfirmed(1L, 2L, 3L, Set.of(4L), at))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void T003_legacyFakeDoesNotInventNewCarePlanDataOrAcceptUnconfiguredLeave() {
        var fake = new FakeCareRequirements();
        var day = LocalDate.of(2026, 10, 28);
        assertThatThrownBy(() -> fake.effectiveOn(1, day)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> fake.waterTargetMl(1, day)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> fake.hasEffectivePlan(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> fake.onApprovedLeave(1, Set.of(2L))).isInstanceOf(IllegalStateException.class);
    }
}
