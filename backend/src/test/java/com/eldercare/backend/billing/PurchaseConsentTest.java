package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.billing.internal.purchase.domain.PurchaseConsent;
import org.junit.jupiter.api.Test;

class PurchaseConsentTest {
    @Test
    void BR_M11_07_thresholdIsInclusiveAndFrozenAtSend() {
        var low = PurchaseConsent.draft(500000).send(500000);
        assertThat(low.status()).isEqualTo("PURCHASE_ALLOWED");
        assertThat(low.allowedAmount()).isEqualTo(500000);
        assertThat(low.threshold()).isEqualTo(500000);
        assertThat(PurchaseConsent.draft(500001).send(500000).status()).isEqualTo("AWAITING_CONSENT");
    }

    @Test
    void FR_023_firstDecisionWinsAndFinalStatesRejectCommands() {
        var waiting = PurchaseConsent.draft(800000).send(500000);
        var agreed = waiting.agree();
        assertThatThrownBy(agreed::agree).isInstanceOf(IllegalStateException.class);
        var rejected = waiting.reject();
        assertThat(rejected.status()).isEqualTo("REJECTED");
        assertThatThrownBy(rejected::cancel).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(agreed.purchase()::cancel).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void Q_259_repeatedAdditionalConsentUsesLatestApprovedAmount() {
        var initial = PurchaseConsent.draft(800000).send(500000).agree();
        var additional = initial.requestAdditional(1000000);
        assertThat(additional.allowedAmount()).isEqualTo(800000);
        assertThat(additional.additionalPending()).isTrue();
        var agreed = additional.agree().requestAdditional(1200000).agree();
        assertThat(agreed.allowedAmount()).isEqualTo(1200000);
        assertThat(agreed.additionalPending()).isFalse();
        assertThatThrownBy(() -> agreed.requestAdditional(1200000)).isInstanceOf(IllegalArgumentException.class);
    }
}
