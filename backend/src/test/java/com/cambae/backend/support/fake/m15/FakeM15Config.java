package com.cambae.backend.support.fake.m15;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả các SPI 002 khai báo cho module khác cài (002 T003; contracts/module-interfaces.md mục 2). Test cần thì
 * {@code @Import(FakeM15Config.class)} rồi tiêm bản giả để đặt dữ liệu; {@code @Primary} để thắng bản thật khi module
 * cài đã có trong context.
 */
@TestConfiguration
public class FakeM15Config {

    @Bean
    @Primary
    FakeStaffProfiles fakeStaffProfiles() {
        return new FakeStaffProfiles();
    }

    @Bean
    @Primary
    FakeLicenses fakeLicenses() {
        return new FakeLicenses();
    }

    @Bean
    @Primary
    FakePracticeScopes fakePracticeScopes() {
        return new FakePracticeScopes();
    }

    @Bean
    @Primary
    FakeAssignments fakeAssignments() {
        return new FakeAssignments();
    }

    @Bean
    @Primary
    FakeDuties fakeDuties() {
        return new FakeDuties();
    }

    @Bean
    @Primary
    FakeBedAllocations fakeBedAllocations() {
        return new FakeBedAllocations();
    }

    @Bean
    @Primary
    FakeFamilyRelationships fakeFamilyRelationships() {
        return new FakeFamilyRelationships();
    }

    @Bean
    @Primary
    FakeResidencyEnds fakeResidencyEnds() {
        return new FakeResidencyEnds();
    }

    @Bean
    @Primary
    FakeResidentProfiles fakeResidentProfiles() {
        return new FakeResidentProfiles();
    }

    @Bean
    @Primary
    FakeFacilityLicenses fakeFacilityLicenses() {
        return new FakeFacilityLicenses();
    }

    @Bean
    @Primary
    FakeFloorAreaNames fakeFloorAreaNames() {
        return new FakeFloorAreaNames();
    }
}
