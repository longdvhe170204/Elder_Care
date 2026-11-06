package com.eldercare.backend.support.fake.billing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Chủ interface E giữ; test dùng @Import(FakeM11Config.class), không đưa vào production. */
@TestConfiguration(proxyBeanMethods = false)
public class FakeM11Config {
    @Bean
    @Primary
    com.eldercare.backend.support.fake.admission.FakeRepresentatives fakeCostRepresentatives() {
        return new com.eldercare.backend.support.fake.admission.FakeRepresentatives();
    }

    @Bean
    @Primary
    FakeFamilyPortalCosts fakeFamilyPortalCosts() {
        return new FakeFamilyPortalCosts();
    }

    @Bean
    @Primary
    FakeFinalPeriodCosts fakeFinalPeriodCosts() {
        return new FakeFinalPeriodCosts();
    }

    @Bean
    @Primary
    FakeCostStatistics fakeCostStatistics() {
        return new FakeCostStatistics();
    }

    @Bean
    @Primary
    FakeCostForBalance fakeCostForBalance() {
        return new FakeCostForBalance();
    }

    @Bean
    @Primary
    FakePurchaseBalance fakePurchaseBalance() {
        return new FakePurchaseBalance();
    }

    @Bean
    FakeCostSources fakeCostSources(org.springframework.context.ApplicationEventPublisher publisher) {
        return new FakeCostSources(publisher);
    }
}
