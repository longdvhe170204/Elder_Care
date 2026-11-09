package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.health.internal.threshold.domain.ThresholdBand;
import com.eldercare.backend.health.internal.threshold.domain.ThresholdRules;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** FR-021: cận trong khoảng vật lý, thứ tự nghiêm ngặt, bỏ qua cận trống. */
class BR_M06_01_ThresholdRulesTest {
    private static final Set<String> COMPONENTS = Set.of("VALUE");
    private final JsonMapper json = JsonMapper.builder().build();
    private final tools.jackson.databind.JsonNode range = json.readTree("{\"VALUE\":{\"min\":30,\"max\":45}}");

    private static ThresholdBand band(
            String dangerBelow, String warningBelow, String warningAbove, String dangerAbove) {
        return new ThresholdBand(dec(dangerBelow), dec(warningBelow), dec(warningAbove), dec(dangerAbove));
    }

    private static BigDecimal dec(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    @Test
    void FR_021_validOrderedBoundsPass() {
        assertThat(ThresholdRules.violations(Map.of("VALUE", band("35", "36", "37.5", "39")), COMPONENTS, range))
                .isEmpty();
    }

    @Test
    void FR_021_emptyBoundsAreSkippedInOrderCheck() {
        assertThat(ThresholdRules.violations(Map.of("VALUE", band(null, "36", null, "39")), COMPONENTS, range))
                .isEmpty();
    }

    @Test
    void FR_020_unknownComponentAndEmptyListAreRejected() {
        assertThat(ThresholdRules.violations(Map.of("OTHER", band(null, "36", null, null)), COMPONENTS, range))
                .isNotEmpty();
        assertThat(ThresholdRules.violations(Map.of(), COMPONENTS, range)).isNotEmpty();
    }
}
