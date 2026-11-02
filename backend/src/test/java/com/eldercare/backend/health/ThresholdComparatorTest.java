package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdComparator;
import java.math.BigDecimal;
import java.util.List;
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
    void FR_013_valuesOutsideWarningAndDangerousBoundsUseTheExpectedLevel() {
        assertThat(level("79.99")).isEqualTo(VitalSignLevel.DANGEROUS);
        assertThat(level("85")).isEqualTo(VitalSignLevel.WARNING);
        assertThat(level("120")).isEqualTo(VitalSignLevel.NORMAL);
        assertThat(level("150")).isEqualTo(VitalSignLevel.WARNING);
        assertThat(level("180.01")).isEqualTo(VitalSignLevel.DANGEROUS);
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

    @Test
    void FR_013_recordUsesTheMostSevereComponent() {
        assertThat(ThresholdComparator.mostSevere(List.of(
                        VitalSignLevel.NORMAL,
                        VitalSignLevel.NO_THRESHOLD,
                        VitalSignLevel.WARNING,
                        VitalSignLevel.DANGEROUS)))
                .isEqualTo(VitalSignLevel.DANGEROUS);
        assertThat(ThresholdComparator.mostSevere(List.of(VitalSignLevel.NORMAL, VitalSignLevel.NO_THRESHOLD)))
                .isEqualTo(VitalSignLevel.NO_THRESHOLD);
    }

    private static VitalSignLevel level(String value) {
        return ThresholdComparator.classify(new BigDecimal(value), BAND);
    }
}
