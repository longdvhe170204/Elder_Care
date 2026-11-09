package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.billing.internal.adjustment.domain.CostAdjustmentRules;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CostAdjustmentRulesTest {
    @Test
    void FR_026_allEffectiveAdjustmentsCountIncludingDrafts() {
        assertThatThrownBy(() -> CostAdjustmentRules.requireNonnegative(
                        new BigDecimal("300000"), new BigDecimal("-300000"), new BigDecimal("-50000")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void FR_026_exactWaiverIsAllowed() {
        assertThatCode(() -> CostAdjustmentRules.requireNonnegative(
                        new BigDecimal("300000"), BigDecimal.ZERO, new BigDecimal("-300000")))
                .doesNotThrowAnyException();
    }

    @Test
    void FR_013_fractionalDongIsRejected() {
        assertThatThrownBy(() ->
                        CostAdjustmentRules.requireNonnegative(BigDecimal.TEN, BigDecimal.ZERO, new BigDecimal("0.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
