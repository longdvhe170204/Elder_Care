package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.billing.internal.purchase.domain.PurchaseExcessCheck;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PurchaseExcessCheckTest {
    @Test
    void BR_62_eitherLimitAllowsRecordingButExcessStillNeedsApprovalReason() {
        assertThat(PurchaseExcessCheck.evaluate(550_000, 500_000, 500_000, new BigDecimal("10")))
                .isEqualTo(PurchaseExcessCheck.Result.EXCESS);
        assertThat(PurchaseExcessCheck.evaluate(450_000, 100_000, 500_000, new BigDecimal("10")))
                .isEqualTo(PurchaseExcessCheck.Result.EXCESS);
        assertThat(PurchaseExcessCheck.evaluate(550_001, 500_000, 500_000, new BigDecimal("10")))
                .isEqualTo(PurchaseExcessCheck.Result.ADDITIONAL_CONSENT_REQUIRED);
        assertThat(PurchaseExcessCheck.evaluate(500_000, 500_000, 500_000, new BigDecimal("10")))
                .isEqualTo(PurchaseExcessCheck.Result.WITHIN_CONSENT);
    }

    @Test
    void Q_259_percentageUsesTheLatestAllowedAmountWithoutRoundingTheBoundary() {
        assertThat(PurchaseExcessCheck.evaluate(660_000, 600_000, 500_000, new BigDecimal("10")))
                .isEqualTo(PurchaseExcessCheck.Result.EXCESS);
        assertThat(PurchaseExcessCheck.evaluate(660_001, 600_000, 500_000, new BigDecimal("10")))
                .isEqualTo(PurchaseExcessCheck.Result.ADDITIONAL_CONSENT_REQUIRED);
    }
}
