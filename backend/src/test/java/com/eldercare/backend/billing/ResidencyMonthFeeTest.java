package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.billing.internal.source.domain.CostSourceCandidate;
import com.eldercare.backend.billing.internal.source.domain.ResidencyMonthFee;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Q-450: nội trú dài hạn giá tháng thu một khoản mỗi tháng; ngắn ngày, giá ngày và tháng có ngày ngắn ngày theo ngày. */
class ResidencyMonthFeeTest {
    private static final YearMonth MONTH = YearMonth.of(2026, 11);

    private CostSourceCandidate day(int day, String type, String period, String blocked, Long existing) {
        LocalDate date = MONTH.atDay(day);
        return new CostSourceCandidate(
                42,
                "RESIDENCY_DAY",
                "42:" + date,
                "DAY",
                date,
                null,
                5L,
                CostType.RESIDENCY_FEE,
                "ngày",
                "Phí lưu trú",
                BigDecimal.ONE,
                new BigDecimal("100"),
                Set.of(),
                existing,
                blocked,
                blocked == null ? null : "lý do",
                Map.of("residenceType", type, "billingPeriod", period));
    }

    private CostSourceCandidate longTerm(int day, String blocked) {
        return day(day, "LONG_TERM_RESIDENTIAL", "MONTH", blocked, null);
    }

    @Test
    void longTermMonthly_oneItemForTheMonth_datedFirstBillableDay_evenWhenRangeIsPartial() {
        var whole = List.of(longTerm(10, "OUTSIDE_BILLING_PERIOD"), longTerm(11, null), longTerm(12, null));

        var selected = ResidencyMonthFee.select(MONTH, whole, whole.subList(2, 3), null);

        assertThat(selected).singleElement().satisfies(c -> {
            assertThat(c.sourceId()).isEqualTo("42:2026-11");
            assertThat(c.billingUnit()).isEqualTo("MONTH");
            assertThat(c.date()).isEqualTo(MONTH.atDay(11));
            assertThat(c.unit()).isEqualTo("tháng");
            assertThat(c.description()).isEqualTo("Phí lưu trú tháng 11/2026");
            assertThat(c.blockedCode()).isNull();
            assertThat(c.existingItemId()).isNull();
        });
    }

    @Test
    void bedReleasedPartway_monthStillCharged_wholeMonthReleased_blocked() {
        var partly = List.of(longTerm(1, null), longTerm(2, "NOT_A_BILLABLE_EVENT"));
        assertThat(ResidencyMonthFee.select(MONTH, partly, partly, 9L))
                .singleElement()
                .satisfies(c -> {
                    assertThat(c.blockedCode()).isNull();
                    assertThat(c.existingItemId()).isEqualTo(9L);
                });

        var all = List.of(longTerm(1, "NOT_A_BILLABLE_EVENT"), longTerm(2, "NOT_A_BILLABLE_EVENT"));
        assertThat(ResidencyMonthFee.select(MONTH, all, all, null))
                .singleElement()
                .satisfies(c -> assertThat(c.blockedCode()).isEqualTo("NOT_A_BILLABLE_EVENT"));
    }

    @Test
    void shortTermAnyDay_orDayPrice_staysDaily() {
        var mixed = List.of(day(1, "SHORT_TERM_RESIDENTIAL", "MONTH", null, null), longTerm(2, null));
        assertThat(ResidencyMonthFee.select(MONTH, mixed, mixed, null)).isEqualTo(mixed);

        var daily = List.of(day(1, "LONG_TERM_RESIDENTIAL", "DAY", null, null));
        assertThat(ResidencyMonthFee.select(MONTH, daily, daily, null)).isEqualTo(daily);
    }

    @Test
    void monthWithExistingDailyItems_staysDaily() {
        var whole = List.of(day(1, "LONG_TERM_RESIDENTIAL", "MONTH", null, 7L), longTerm(2, null));

        assertThat(ResidencyMonthFee.select(MONTH, whole, whole, null)).isEqualTo(whole);
    }

    @Test
    void monthSwitchedToDaily_afterMonthItem_cancelsMonthSourceAndHoldsDailyOnes() {
        var mixed = List.of(longTerm(1, null), day(2, "SHORT_TERM_RESIDENTIAL", "MONTH", null, null));

        var selected = ResidencyMonthFee.select(MONTH, mixed, mixed, 9L);

        assertThat(selected).hasSize(3);
        assertThat(selected.getFirst().billingUnit()).isEqualTo("MONTH");
        assertThat(selected.getFirst().blockedCode()).isEqualTo("NOT_A_BILLABLE_EVENT");
        assertThat(selected.subList(1, 3))
                .allSatisfy(c -> assertThat(c.blockedCode()).isEqualTo("COST_ITEM_ALREADY_EXISTS"));
    }
}
