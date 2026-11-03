package com.eldercare.backend.support.fake.health;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/** Một bộ fake dùng chung; chỉ nạp khi test import cấu hình. */
@TestConfiguration(proxyBeanMethods = false)
@Import(FakeVitalSigns.Config.class)
public class FakeHealthConfig {
    @Bean
    @Primary
    public FakeMeasurementSchedules fakeMeasurementSchedules() {
        return new FakeMeasurementSchedules();
    }

    @Bean
    @Primary
    public FakeFacilityContact fakeFacilityContact() {
        return new FakeFacilityContact();
    }

    @Bean
    @Primary
    public FakeVitalSignStatistics fakeVitalSignStatistics() {
        return new FakeVitalSignStatistics();
    }

    @Bean
    public FakeHealthEvents fakeHealthEvents(ApplicationEventPublisher publisher) {
        return new FakeHealthEvents(publisher);
    }
}
