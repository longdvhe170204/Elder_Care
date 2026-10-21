package com.cambae.backend.support.fake.m03;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả do 003 giữ (T002); test dùng {@code @Import(FakeM03Config.class)} và đặt dữ liệu qua từng bean.
 * Không được nạp bằng component scan của ứng dụng chính.
 */
@TestConfiguration
public class FakeM03Config {

    @Bean
    @Primary
    FakeAllocationResidencies fakeAllocationResidencies() {
        return new FakeAllocationResidencies();
    }

    @Bean
    @Primary
    FakeDayCareAttendances fakeDayCareAttendances() {
        return new FakeDayCareAttendances();
    }

    @Bean
    @Primary
    FakeInfectionStatuses fakeInfectionStatuses() {
        return new FakeInfectionStatuses();
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
