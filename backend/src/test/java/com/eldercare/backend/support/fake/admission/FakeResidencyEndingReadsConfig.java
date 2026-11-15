package com.eldercare.backend.support.fake.admission;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Test của 017 dùng {@code @Import(FakeResidencyEndingReadsConfig.class)} (bản giả do chủ 004 viết; 017 T006). */
@TestConfiguration(proxyBeanMethods = false)
public class FakeResidencyEndingReadsConfig {
    @Bean
    @Primary
    public FakeResidencyEndingReads fakeResidencyEndingReads() {
        return new FakeResidencyEndingReads();
    }
}
