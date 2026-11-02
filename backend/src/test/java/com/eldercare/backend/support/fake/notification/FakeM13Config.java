package com.eldercare.backend.support.fake.notification;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả các SPI 009 khai báo (009 T003; contracts/module-interfaces.md mục 2.1). Test cần thì
 * {@code @Import(FakeM13Config.class)} rồi tiêm bản giả để đặt dữ liệu; {@code @Primary} để thắng bản thật.
 */
@TestConfiguration
public class FakeM13Config {

    @Bean
    @Primary
    FakeFamilyNotifications fakeFamilyNotifications() {
        return new FakeFamilyNotifications();
    }

    @Bean
    @Primary
    FakeSmsProvider fakeSmsProvider() {
        return new FakeSmsProvider();
    }
}
