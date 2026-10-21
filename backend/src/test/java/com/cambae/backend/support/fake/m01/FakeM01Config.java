package com.cambae.backend.support.fake.m01;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả các SPI mà M01 khai báo cho module khác cài (001 T002; contracts/module-interfaces.md mục 2). Test cần thì
 * {@code @Import(FakeM01Config.class)} rồi tiêm bản giả để đặt dữ liệu; {@code @Primary} để thắng bản thật khi module
 * cài đã có trong context. {@link FakeStatusTransitionParticipant} không {@code @Primary}: nhiều bean cùng tồn tại là
 * đúng thiết kế.
 */
@TestConfiguration
public class FakeM01Config {

    @Bean
    FakeStatusTransitionParticipant fakeStatusTransitionParticipant() {
        return new FakeStatusTransitionParticipant();
    }

    @Bean
    @Primary
    FakeCareLevelChangeRequests fakeCareLevelChangeRequests() {
        return new FakeCareLevelChangeRequests();
    }

    @Bean
    @Primary
    FakeContracts fakeContracts() {
        return new FakeContracts();
    }

    @Bean
    @Primary
    FakeAbsences fakeAbsences() {
        return new FakeAbsences();
    }
}
