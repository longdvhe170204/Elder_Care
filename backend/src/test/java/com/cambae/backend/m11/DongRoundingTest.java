package com.cambae.backend.m11;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m11.pricing.internal.domain.DongRounding;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DongRoundingTest {
    @ParameterizedTest
    @CsvSource({
        "0, 0",
        "0.49, 0",
        "0.5, 1",
        "1.5, 2",
        "1.51, 2",
        "-0.49, 0",
        "-0.5, -1",
        "-1.5, -2",
        "-1.51, -2",
        "161538.3, 161538",
        "69230.7, 69231",
        "123000000000000.5, 123000000000001"
    })
    void FR_013_halfUpUsesAbsoluteValueAndKeepsWholeDong(String input, String expected) {
        var actual = DongRounding.round(new BigDecimal(input));
        assertThat(actual).isEqualByComparingTo(expected);
        assertThat(actual.scale()).isZero();
    }
}
