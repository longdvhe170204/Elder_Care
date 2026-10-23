package com.cambae.backend.support.fake.m07;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Bản giả dùng chung, chỉ được nạp khi test import. */
@TestConfiguration(proxyBeanMethods = false)
public class FakeM07Config {
    @Bean
    @Primary
    public FakeNursingShifts fakeNursingShifts() {
        return new FakeNursingShifts();
    }
}
