package com.eldercare.backend.support.fake.medication;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả các SPI mà Module M07 khai báo cho module khác cài (contracts/module-interfaces.md mục 2.1).
 * Test cần thì {@code @Import(FakeM07Config.class)} rồi tiêm bản giả để đặt dữ liệu test;
 * {@code @Primary} để thắng bản thật khi module cài đã có trong context.
 */
@TestConfiguration
public class FakeM07Config {

    @Bean
    @Primary
    public FakeShiftNursePort fakeShiftNursePort() {
        return new FakeShiftNursePort();
    }

    @Bean
    @Primary
    public FakeTripMedicationPort fakeTripMedicationPort() {
        return new FakeTripMedicationPort();
    }

    @Bean
    @Primary
    public FakePurchaseAssistancePort fakePurchaseAssistancePort() {
        return new FakePurchaseAssistancePort();
    }

    @Bean
    @Primary
    public FakeProcurementMedicationPort fakeProcurementMedicationPort() {
        return new FakeProcurementMedicationPort();
    }
}
