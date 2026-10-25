package com.cambae.backend.support.fake.m02;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

@TestConfiguration(proxyBeanMethods = false)
public class FakeM02Config {
    @Bean
    @Primary
    public FakeContractSchedules fakeContractSchedules() {
        return new FakeContractSchedules();
    }
}
