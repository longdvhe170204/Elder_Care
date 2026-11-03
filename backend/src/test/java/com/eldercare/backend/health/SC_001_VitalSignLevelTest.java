package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdComparator;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SC_001_VitalSignLevelTest {

    private record Case(ThresholdBand band, List<VitalSignLevel> expected) {}

    @Test
    void FR_013_SC_001_threeHundredValuesMatchTheReviewedLevelTable() {
        var cases = List.of(
                new Case(new ThresholdBand(decimal(5), decimal(10), decimal(30), decimal(40)), expected(5, 10, 31, 41)),
                new Case(
                        new ThresholdBand(null, decimal(10), decimal(20), decimal(30)),
                        expectedWithoutLowerDanger(10, 21, 31)),
                new Case(
                        new ThresholdBand(decimal(5), null, decimal(25), decimal(35)),
                        expectedWithoutLowerWarning(5, 26, 36)),
                new Case(
                        new ThresholdBand(decimal(5), decimal(10), null, decimal(40)),
                        expectedWithoutUpperWarning(5, 10, 41)),
                new Case(new ThresholdBand(null, null, decimal(30), decimal(40)), expectedWithoutLowerBounds(31, 41)),
                new Case(ThresholdBand.none(), repeated(VitalSignLevel.NO_THRESHOLD, 50)));

        int checked = 0;
        for (Case testCase : cases) {
            for (int value = 0; value < 50; value++) {
                assertThat(ThresholdComparator.classify(decimal(value), testCase.band()))
                        .as("band=%s, value=%s", testCase.band(), value)
                        .isEqualTo(testCase.expected().get(value));
                checked++;
            }
        }
        assertThat(checked).isEqualTo(300);
    }

    private static List<VitalSignLevel> expected(
            int lowDangerEnd, int normalStart, int highWarningStart, int highDangerStart) {
        return concat(
                repeated(VitalSignLevel.DANGEROUS, lowDangerEnd),
                repeated(VitalSignLevel.WARNING, normalStart - lowDangerEnd),
                repeated(VitalSignLevel.NORMAL, highWarningStart - normalStart),
                repeated(VitalSignLevel.WARNING, highDangerStart - highWarningStart),
                repeated(VitalSignLevel.DANGEROUS, 50 - highDangerStart));
    }

    private static List<VitalSignLevel> expectedWithoutLowerDanger(
            int normalStart, int highWarningStart, int highDangerStart) {
        return concat(
                repeated(VitalSignLevel.WARNING, normalStart),
                repeated(VitalSignLevel.NORMAL, highWarningStart - normalStart),
                repeated(VitalSignLevel.WARNING, highDangerStart - highWarningStart),
                repeated(VitalSignLevel.DANGEROUS, 50 - highDangerStart));
    }

    private static List<VitalSignLevel> expectedWithoutLowerWarning(
            int normalStart, int highWarningStart, int highDangerStart) {
        return concat(
                repeated(VitalSignLevel.DANGEROUS, normalStart),
                repeated(VitalSignLevel.NORMAL, highWarningStart - normalStart),
                repeated(VitalSignLevel.WARNING, highDangerStart - highWarningStart),
                repeated(VitalSignLevel.DANGEROUS, 50 - highDangerStart));
    }

    private static List<VitalSignLevel> expectedWithoutUpperWarning(
            int warningStart, int normalStart, int highDangerStart) {
        return concat(
                repeated(VitalSignLevel.DANGEROUS, warningStart),
                repeated(VitalSignLevel.WARNING, normalStart - warningStart),
                repeated(VitalSignLevel.NORMAL, highDangerStart - normalStart),
                repeated(VitalSignLevel.DANGEROUS, 50 - highDangerStart));
    }

    private static List<VitalSignLevel> expectedWithoutLowerBounds(int highWarningStart, int highDangerStart) {
        return concat(
                repeated(VitalSignLevel.NORMAL, highWarningStart),
                repeated(VitalSignLevel.WARNING, highDangerStart - highWarningStart),
                repeated(VitalSignLevel.DANGEROUS, 50 - highDangerStart));
    }

    @SafeVarargs
    private static List<VitalSignLevel> concat(List<VitalSignLevel>... parts) {
        return java.util.stream.Stream.of(parts).flatMap(List::stream).toList();
    }

    private static List<VitalSignLevel> repeated(VitalSignLevel level, int count) {
        return java.util.Collections.nCopies(count, level);
    }

    private static BigDecimal decimal(int value) {
        return BigDecimal.valueOf(value);
    }
}
