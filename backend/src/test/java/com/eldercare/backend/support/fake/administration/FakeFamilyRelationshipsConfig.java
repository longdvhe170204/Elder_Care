package com.eldercare.backend.support.fake.administration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả {@code FamilyRelationshipApi} (002 T003) tách khỏi {@link FakeM15Config} khi 012 đã có bản cài thật (012 T038,
 * 2026-11-06): test không import config này dùng bản thật của 012 (dữ liệu người thân, quan hệ thật). Test còn đặt dữ
 * liệu vào bản giả thì {@code @Import} thêm config này; {@code @Primary} để thắng bản thật.
 */
@TestConfiguration
public class FakeFamilyRelationshipsConfig {

    @Bean
    @Primary
    FakeFamilyRelationships fakeFamilyRelationships() {
        return new FakeFamilyRelationships();
    }
}
