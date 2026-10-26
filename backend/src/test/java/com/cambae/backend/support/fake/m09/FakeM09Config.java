package com.cambae.backend.support.fake.m09;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** T002: test dùng @Import(FakeM09Config.class), tiêm bản giả để đặt dữ liệu; không nạp vào ứng dụng thật. */
@TestConfiguration(proxyBeanMethods = false)
public class FakeM09Config {

    @Bean
    @Primary
    FakeShifts fakeShifts() {
        return new FakeShifts();
    }

    @Bean
    @Primary
    FakeStaffStatistics fakeStaffStatistics() {
        return new FakeStaffStatistics();
    }

    @Bean
    @Primary
    FakeHeldBelongings fakeHeldBelongings() {
        return new FakeHeldBelongings();
    }

    @Bean
    @Primary
    FakeTripDuties fakeTripDuties() {
        return new FakeTripDuties();
    }

    @Bean
    @Primary
    FakeExtendedPublicationChecks fakeExtendedPublicationChecks() {
        return new FakeExtendedPublicationChecks();
    }
}
