package com.cambae.backend.support.fake.m02;

import com.cambae.backend.m02.api.dto.PostDeathItemCode;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả các SPI mà M02 khai báo cho module khác cài (004 T002; contracts/module-interfaces.md mục 2.1). Test cần thì
 * {@code @Import(FakeM02Config.class)} rồi tiêm bản giả để đặt dữ liệu; {@code @Primary} để thắng bản thật khi module
 * cài đã có trong context. Năm bean {@link FakePostDeathItem} (mỗi mã một bean) không {@code @Primary}: nhiều bean cùng
 * tồn tại là đúng thiết kế.
 */
@TestConfiguration
public class FakeM02Config {

    @Bean
    @Primary
    FakeRepresentatives fakeRepresentatives() {
        return new FakeRepresentatives();
    }

    @Bean
    @Primary
    FakePickupRecords fakePickupRecords() {
        return new FakePickupRecords();
    }

    @Bean
    @Primary
    FakeDepositStatuses fakeDepositStatuses() {
        return new FakeDepositStatuses();
    }

    @Bean
    @Primary
    FakeFinalPeriods fakeFinalPeriods() {
        return new FakeFinalPeriods();
    }

    @Bean
    @Primary
    FakeDayCareAttendance fakeDayCareAttendance() {
        return new FakeDayCareAttendance();
    }

    @Bean
    FakePostDeathItem fakeBelongingsItem() {
        return new FakePostDeathItem(PostDeathItemCode.BELONGINGS);
    }

    @Bean
    FakePostDeathItem fakeStoredMedicationItem() {
        return new FakePostDeathItem(PostDeathItemCode.STORED_MEDICATION);
    }

    @Bean
    FakePostDeathItem fakeFinalPeriodCostsItem() {
        return new FakePostDeathItem(PostDeathItemCode.FINAL_PERIOD_COSTS);
    }

    @Bean
    FakePostDeathItem fakeAlertsIncidentsItem() {
        return new FakePostDeathItem(PostDeathItemCode.ALERTS_INCIDENTS);
    }

    @Bean
    FakePostDeathItem fakeSettlementItem() {
        return new FakePostDeathItem(PostDeathItemCode.SETTLEMENT);
    }
}
