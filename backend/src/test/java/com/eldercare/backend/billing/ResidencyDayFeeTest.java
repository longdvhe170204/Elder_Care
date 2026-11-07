package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.pricing.domain.ResidencyDayFee;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ResidencyDayFeeTest {
    static Stream<YearMonth> months() {
        return Stream.concat(
                IntStream.rangeClosed(1, 12).mapToObj(month -> YearMonth.of(2026, month)),
                Stream.of(YearMonth.of(2028, 2)));
    }

    @ParameterizedTest
    @MethodSource("months")
    void BR_M11_09_twelveMonthsAndLeapFebruarySumToTheExactMonthlyPrice(YearMonth month) {
        var price = new BigDecimal("9300001");
        var total = IntStream.rangeClosed(1, month.lengthOfMonth())
                .mapToObj(day -> ResidencyDayFee.monthly(price, month.atDay(day)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(price);
    }

    @Test
    void Q_395_residencyFeeIsTheFullRoundedBase() {
        var date = LocalDate.of(2026, 10, 1);
        assertThat(ResidencyDayFee.base(new BigDecimal("9300047"), date)).isEqualByComparingTo("300002");
        assertThat(ResidencyDayFee.monthly(new BigDecimal("9300047"), date)).isEqualByComparingTo("300002");
    }

    @Test
    void FR_008_eachDateUsesItsProvidedEffectiveContractPrice() {
        assertThat(ResidencyDayFee.monthly(new BigDecimal("9000000"), LocalDate.of(2026, 11, 19)))
                .isEqualByComparingTo("300000");
        assertThat(ResidencyDayFee.monthly(new BigDecimal("12000000"), LocalDate.of(2026, 11, 20)))
                .isEqualByComparingTo("400000");
    }
}
