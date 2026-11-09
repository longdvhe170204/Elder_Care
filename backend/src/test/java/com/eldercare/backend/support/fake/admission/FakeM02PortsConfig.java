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
     * Bản thật cùng mã (013 {@code BelongingPostDeathItem}, 007 {@code AlertsIncidentsPostDeathItem}, sau này 006, 010,
     * 017): test 004 đặt trạng thái qua bản giả nên năm bản giả đều đứng trước ({@code orderedStream}, mục đầu thắng).
     */
    @Bean
    @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
    FakePostDeathItem fakeBelongingsItem() {
        return new FakePostDeathItem(PostDeathItemCode.BELONGINGS);
    }

    @Bean
    @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
    FakePostDeathItem fakeStoredMedicationItem() {
        return new FakePostDeathItem(PostDeathItemCode.STORED_MEDICATION);
    }

    @Bean
    @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
    FakePostDeathItem fakeFinalPeriodCostsItem() {
        return new FakePostDeathItem(PostDeathItemCode.FINAL_PERIOD_COSTS);
    }

    @Bean
    @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
    FakePostDeathItem fakeAlertsIncidentsItem() {
        return new FakePostDeathItem(PostDeathItemCode.ALERTS_INCIDENTS);
    }

    @Bean
    @org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
    FakePostDeathItem fakeSettlementItem() {
        return new FakePostDeathItem(PostDeathItemCode.SETTLEMENT);
    }
}
