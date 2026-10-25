package com.cambae.backend.support.fake.m03;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** T002: test dùng @Import(FakeM03Config.class), tiêm bản giả để đặt dữ liệu; không quét vào ứng dụng thật. */
@TestConfiguration
public class FakeM03Config {
    @Bean
    @Primary
    FakeAllocationResidencySource fakeAllocationResidencySource() {
        return new FakeAllocationResidencySource();
    }

    @Bean
    @Primary
    FakeDayCareArrivals fakeDayCareArrivals() {
        return new FakeDayCareArrivals();
    }

    @Bean
    @Primary
    FakeAllocationInfectionStatus fakeAllocationInfectionStatus() {
        return new FakeAllocationInfectionStatus();
    }

    @Bean
    @Primary
    FakeLocationReferences fakeLocationReferences() {
        return new FakeLocationReferences();
    }

    @Bean
    @Primary
    FakeBedAssets fakeBedAssets() {
        return new FakeBedAssets();
    }
}
