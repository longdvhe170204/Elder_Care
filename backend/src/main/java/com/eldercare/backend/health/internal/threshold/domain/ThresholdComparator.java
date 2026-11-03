package com.eldercare.backend.health.internal.threshold.domain;

import com.eldercare.backend.health.api.dto.VitalSignLevel;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/** Hàm thuần so giá trị với ngưỡng và lấy mức nặng nhất của bản ghi (FR-013, FR-024). */
public final class ThresholdComparator {

    private ThresholdComparator() {}

    public static VitalSignLevel classify(BigDecimal value, ThresholdBand band) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(band, "band");
        if (!band.hasAnyBound()) {
            return VitalSignLevel.NO_THRESHOLD;
        }
        if (isBelow(value, band.lowerDangerous()) || isAbove(value, band.upperDangerous())) {
            return VitalSignLevel.DANGEROUS;
        }
        if (isBelow(value, band.lowerWarning()) || isAbove(value, band.upperWarning())) {
            return VitalSignLevel.WARNING;
        }
        return VitalSignLevel.NORMAL;
    }

    public static VitalSignLevel mostSevere(Collection<VitalSignLevel> componentLevels) {
        Objects.requireNonNull(componentLevels, "componentLevels");
        return componentLevels.stream()
                .max(Comparator.comparingInt(ThresholdComparator::severity))
                .orElseThrow(() -> new IllegalArgumentException("Bản ghi chỉ số phải có ít nhất một thành phần"));
    }

    private static boolean isBelow(BigDecimal value, BigDecimal bound) {
        return bound != null && value.compareTo(bound) < 0;
    }

    private static boolean isAbove(BigDecimal value, BigDecimal bound) {
        return bound != null && value.compareTo(bound) > 0;
    }

    private static int severity(VitalSignLevel level) {
        return switch (level) {
            case NORMAL -> 0;
            case NO_THRESHOLD -> 1;
            case WARNING -> 2;
            case DANGEROUS -> 3;
        };
    }
}
