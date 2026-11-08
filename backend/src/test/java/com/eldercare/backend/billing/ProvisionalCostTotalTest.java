package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.internal.provision.domain.ProvisionalCostTotal;
import org.junit.jupiter.api.Test;

class ProvisionalCostTotalTest {
    @Test
    void FR_038_excludesUnapprovedManualAndPurchaseAndCancelledItems() {
        assertThat(ProvisionalCostTotal.included("MANUAL", "DRAFT")).isFalse();
        assertThat(ProvisionalCostTotal.included("PURCHASE_ASSISTANCE", "CHECKED"))
                .isFalse();
        assertThat(ProvisionalCostTotal.included("EVENT_BASED", "CANCELLED")).isFalse();
        assertThat(ProvisionalCostTotal.included("EVENT_BASED", "FINALIZED")).isFalse();
        assertThat(ProvisionalCostTotal.included("MANUAL", "APPROVED")).isTrue();
        assertThat(ProvisionalCostTotal.included("EVENT_BASED", "DRAFT")).isTrue();
        assertThat(ProvisionalCostTotal.included("ADJUSTMENT", "CHECKED")).isTrue();
    }
}
