package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.belongings.internal.inventorycheck.domain.InventoryPeriod;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** 013 T023 (BR-M12-07, FR-028a, FR-028c, CFG-M12-04): kỳ kiểm kê và hạn hoàn thành. */
class InventoryPeriodTest {

    @Test
    void monthlyCycleGivesTheMonthAsPeriodCodeAndThreeDayDeadline() {
        InventoryPeriod period = InventoryPeriod.of(LocalDate.of(2026, 10, 17), 1, "MONTHS", 3);

        assertThat(period.code()).isEqualTo("2026-10");
        assertThat(period.start()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(period.dueDate()).isEqualTo(LocalDate.of(2026, 10, 3));
        assertThat(period.overdueOn(LocalDate.of(2026, 10, 3))).isFalse();
        assertThat(period.overdueOn(LocalDate.of(2026, 10, 4))).isTrue();
    }

    @Test
    void everyDayOfTheSameMonthHasTheSamePeriodCode() {
        assertThat(InventoryPeriod.of(LocalDate.of(2026, 10, 1), 1, "MONTHS", 3).code())
                .isEqualTo(InventoryPeriod.of(LocalDate.of(2026, 10, 31), 1, "MONTHS", 3)
                        .code());
        assertThat(InventoryPeriod.of(LocalDate.of(2026, 11, 1), 1, "MONTHS", 3).code())
                .isEqualTo("2026-11");
    }

    @Test
    void dailyCycleUsesTheStartDateAsCode() {
        InventoryPeriod period = InventoryPeriod.of(LocalDate.of(2026, 10, 17), 7, "DAYS", 2);

        assertThat(period.start().toEpochDay() % 7).isZero();
        assertThat(period.code()).isEqualTo(period.start().toString());
        assertThat(period.dueDate()).isEqualTo(period.start().plusDays(1));
    }
}
