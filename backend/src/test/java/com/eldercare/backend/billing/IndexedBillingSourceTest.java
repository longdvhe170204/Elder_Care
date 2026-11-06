package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.billing.internal.source.domain.IndexedBillingSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IndexedBillingSourceTest {
    private IndexedBillingSource source(String quantity) {
        return new IndexedBillingSource(
                "DOSE",
                "123",
                "45",
                67,
                LocalDate.of(2026, 11, 4),
                Instant.parse("2026-11-04T01:00:00Z"),
                null,
                45L,
                new BigDecimal(quantity));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.125", "1.2500", "999999999.999"})
    void FR_005_supportedSourceQuantityIsPreservedWithoutRounding(String quantity) {
        assertThat(source(quantity).quantity()).isEqualByComparingTo(quantity);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.1234", "1000000000"})
    void FR_005_unrepresentableSourceQuantityIsRejectedInsteadOfRounded(String quantity) {
        assertThatThrownBy(() -> source(quantity)).isInstanceOf(IllegalArgumentException.class);
    }
}
