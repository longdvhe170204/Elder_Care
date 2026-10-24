package com.cambae.backend.support.fake.m01;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

@TestConfiguration(proxyBeanMethods = false)
public class FakeM01Config {
    @Bean
    @Primary
    public FakeResidents fakeResidents() {
        return new FakeResidents();
    }
}
