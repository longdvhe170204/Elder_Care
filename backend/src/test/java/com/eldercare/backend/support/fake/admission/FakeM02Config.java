package com.eldercare.backend.support.fake.admission;

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
