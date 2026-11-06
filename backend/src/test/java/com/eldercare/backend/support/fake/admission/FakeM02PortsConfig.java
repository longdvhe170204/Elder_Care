package com.eldercare.backend.support.fake.admission;

import com.eldercare.backend.admission.api.dto.PostDeathItemCode;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Bản giả các SPI mà M02 khai báo cho module khác cài (004 T002; contracts/module-interfaces.md mục 2.1). Test cần thì
 * {@code @Import(FakeM02PortsConfig.class)} rồi tiêm bản giả để đặt dữ liệu; {@code @Primary} để thắng bản thật khi module
 * cài đã có trong context. Năm bean {@link FakePostDeathItem} (mỗi mã một bean) không {@code @Primary}: nhiều bean cùng
 * tồn tại là đúng thiết kế.
 */
@TestConfiguration
public class FakeM02PortsConfig {

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

    /**
     * Bản thật của 013 ({@code BelongingPostDeathItem}) cùng mã {@code BELONGINGS}: test 004 đặt trạng thái qua bản giả nên
     * bản giả đứng trước ({@code orderedStream}, mục đầu thắng) — 013 T030.
     */
    @Bean
    @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
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
