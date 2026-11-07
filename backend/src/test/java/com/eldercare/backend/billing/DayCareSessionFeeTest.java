package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.billing.internal.pricing.domain.DayCareSessionFee;
import java.math.BigDecimal;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DayCareSessionFeeTest {
    @ParameterizedTest
    @CsvSource({"0, 0", "50, 400001", "100, 800001"})
    void Q_395_dayCareKeepsTheSourceConfiguredPercentage(String percent, String expected) {
        assertThat(DayCareSessionFee.direct(new BigDecimal("800001"), new BigDecimal(percent)))
                .isEqualByComparingTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 12, 13, 22, 23, 31})
    void BR_M11_09_scheduledSessionsSumToTheExactMonthlyPrice(int scheduledCount) {
        var price = new BigDecimal("10000000");
        var total = IntStream.rangeClosed(1, scheduledCount)
                .mapToObj(session -> DayCareSessionFee.monthly(
                        price, scheduledCount, session == scheduledCount, new BigDecimal("100")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(price);
    }

    @Test
    void FR_009_lastScheduledSessionGetsTheRemainder() {
        var price = new BigDecimal("10000000");
        assertThat(DayCareSessionFee.base(price, 13, false)).isEqualByComparingTo("769231");
        assertThat(DayCareSessionFee.base(price, 13, true)).isEqualByComparingTo("769228");
    }
}
