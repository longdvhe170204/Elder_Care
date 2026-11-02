package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BR_M04_08_WaterIntakeRuleTest {
    @Test
    void BR_M04_08_proratesByMinutesFloorsMlAndUsesStrictThreshold() throws Exception {
        var rule = Class.forName("com.eldercare.backend.care.internal.rules.domain.WaterIntakeRule");
        var target = rule.getMethod("adjustedTarget", int.class, long.class, long.class);
        assertThat(target.invoke(null, 1500, 960L, 960L)).isEqualTo(1500);
        assertThat(target.invoke(null, 1500, 61L, 960L)).isEqualTo(95);
        assertThat(target.invoke(null, 1500, 0L, 960L)).isEqualTo(0);
        var low = rule.getMethod("isLow", BigDecimal.class, int.class, BigDecimal.class);
        assertThat(low.invoke(null, new BigDecimal("899"), 1500, new BigDecimal("60")))
                .isEqualTo(true);
        assertThat(low.invoke(null, new BigDecimal("900"), 1500, new BigDecimal("60")))
                .isEqualTo(false);
    }
}
