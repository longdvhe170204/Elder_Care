package com.eldercare.backend.support.fake.care;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Bản giả dùng chung, chỉ được nạp khi test import. */
@TestConfiguration(proxyBeanMethods = false)
public class FakeM04Config {
    @Bean
    @Primary
    public FakeCareRequirements fakeCareRequirements() {
        return new FakeCareRequirements();
    }

    @Bean
    @Primary
    public FakeCareShifts fakeCareShifts() {
        return new FakeCareShifts();
    }
}
