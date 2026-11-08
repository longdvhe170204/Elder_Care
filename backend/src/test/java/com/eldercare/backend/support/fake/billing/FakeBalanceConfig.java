package com.eldercare.backend.support.fake.billing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Bản giả của 017 (A giữ, module-interfaces 017 mục 2.2, 2.3): test của 012, 016 dùng {@code @Import(FakeBalanceConfig.class)}. */
@TestConfiguration(proxyBeanMethods = false)
public class FakeBalanceConfig {

    @Bean
    @Primary
    FakeBalances fakeBalances() {
        return new FakeBalances();
    }

    @Bean
    @Primary
    FakeBalanceStatistics fakeBalanceStatistics() {
        return new FakeBalanceStatistics();
    }
}
