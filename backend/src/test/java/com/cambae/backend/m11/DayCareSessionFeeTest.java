package com.cambae.backend.m11;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.m11.pricing.internal.domain.DayCareSessionFee;
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

    @Test
    void FR_009_roundsTheSessionBaseBeforeApplyingAttendancePercentage() {
        assertThat(DayCareSessionFee.monthly(new BigDecimal("3900020"), 13, false, new BigDecimal("30")))
                .isEqualByComparingTo("90001");
        assertThat(DayCareSessionFee.monthly(new BigDecimal("3900020"), 13, false, BigDecimal.ZERO))
                .isEqualByComparingTo("0");
    }

    @Test
    void FR_009_providedScheduledCountAndDirectAdditionalSessionPriceStaySeparate() {
        // Nguồn 004 cung cấp số buổi sau lọc ngày khu nghỉ; tính lại dùng số mới, không số ngày trong tháng.
        var price = new BigDecimal("10000000");
        assertThat(DayCareSessionFee.base(price, 12, false)).isEqualByComparingTo("833333");
        assertThat(DayCareSessionFee.base(price, 13, false)).isEqualByComparingTo("769231");
        // Buổi phát sinh dùng giá trực tiếp 100%, không nhận phần dư của giá tháng.
        assertThat(DayCareSessionFee.direct(new BigDecimal("800001"), new BigDecimal("100")))
                .isEqualByComparingTo("800001");
    }

    @Test
    void FR_009_monthlyDivisionRequiresAScheduledSession() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DayCareSessionFee.base(new BigDecimal("10000000"), 0, false));
    }
}
