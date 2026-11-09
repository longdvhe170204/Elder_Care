package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdComparator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ThresholdComparatorTest {

    private static final ThresholdBand BAND =
            new ThresholdBand(new BigDecimal("80"), new BigDecimal("90"), new BigDecimal("140"), new BigDecimal("180"));

    @Test
    void FR_024_equalBoundaryDoesNotExceedThatBoundary() {
        assertThat(level("80")).isEqualTo(VitalSignLevel.WARNING);
        assertThat(level("90")).isEqualTo(VitalSignLevel.NORMAL);
        assertThat(level("140")).isEqualTo(VitalSignLevel.NORMAL);
        assertThat(level("180")).isEqualTo(VitalSignLevel.WARNING);
    }

    @Test
    void FR_024_missingBoundsAreIgnoredAndMissingBandIsReported() {
        var onlyLower = new ThresholdBand(null, new BigDecimal("94"), null, null);
        assertThat(ThresholdComparator.classify(new BigDecimal("93"), onlyLower))
                .isEqualTo(VitalSignLevel.WARNING);
        assertThat(ThresholdComparator.classify(new BigDecimal("94"), onlyLower))
                .isEqualTo(VitalSignLevel.NORMAL);
        assertThat(ThresholdComparator.classify(new BigDecimal("96"), ThresholdBand.none()))
                .isEqualTo(VitalSignLevel.NO_THRESHOLD);
    }

    private static VitalSignLevel level(String value) {
        return ThresholdComparator.classify(new BigDecimal(value), BAND);
    }
}
