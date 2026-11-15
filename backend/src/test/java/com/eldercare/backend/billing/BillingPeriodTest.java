package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.billing.internal.pricing.domain.BillingPeriod;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** T006: khoảng tính phí thuần, ngày nghiệp vụ do bên gọi lấy từ nguồn 004/001. */
class BillingPeriodTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate START = LocalDate.of(2026, 10, 5);
    private static final LocalDate ADMITTED = START.plusDays(5);

    private static Instant at(String local) {
        return LocalDateTime.parse(local).atZone(ZONE).toInstant();
    }

    @Test
    void Q_22_noCompletedAdmissionKeepsTheNotYetAdmittedFlag() {
        var period = BillingPeriod.residential(START, null, null, null, null);
        assertThat(period.contains(at("2026-10-06T12:00:00"), ZONE)).isTrue();
        assertThat(period.notYetAdmitted(START.plusDays(1))).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "2026-10-04T23:59:59.999999,false",
        "2026-10-05T00:00:00,true",
        "2026-10-20T23:59:59.999999,true",
        "2026-10-21T00:00:00,false"
    })
    @Test
    void Q_27_cancellationKeepsTheWholeCancellationDay() {
        var cancellation = LocalDate.of(2026, 10, 15);
        var period = BillingPeriod.residential(START, ADMITTED, null, cancellation, null);
        assertThat(period.contains(at("2026-10-15T23:59:59.999999"), ZONE)).isTrue();
        assertThat(period.contains(at("2026-10-16T00:00:00"), ZONE)).isFalse();
    }

    @Test
    void FR_036_residencyFeeStillIncludesTheDeathDay() {
        var death = at("2026-10-15T10:30:00");
        var period = BillingPeriod.residential(START, ADMITTED, null, null, death);
        assertThat(period.containsDay(LocalDate.of(2026, 10, 15), ZONE)).isTrue();
        assertThat(period.containsDay(LocalDate.of(2026, 10, 16), ZONE)).isFalse();
        assertThat(period.contains(at("2026-10-15T11:00:00"), ZONE)).isFalse();
    }
}
