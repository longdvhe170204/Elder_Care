package com.cambae.backend.support.fake.m03;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Bản giả dùng chung, chỉ được nạp khi test import. */
@TestConfiguration(proxyBeanMethods = false)
public class FakeM03Config {
    @Bean
    @Primary
    public FakeLocationReferences fakeLocationReferences() {
        return new FakeLocationReferences();
    }
}
