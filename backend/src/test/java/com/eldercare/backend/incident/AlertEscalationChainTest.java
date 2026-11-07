package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.alert.domain.AlertEscalationChain;
import org.junit.jupiter.api.Test;

/** Chuỗi leo thang ba cấp (FR-033, research S-08): lên một cấp, bỏ qua cấp không có ai, cấp 2 là tối đa. */
class AlertEscalationChainTest {
    @Test
    void BR_M05_01_levelZeroGoesToFloorManagerAndDoctors() {
        assertThat(AlertEscalationChain.next(0, level -> true)).hasValue(1);
    }

    @Test
    void BR_M05_01_levelOneGoesToManager() {
        assertThat(AlertEscalationChain.next(1, level -> true)).hasValue(2);
    }

    @Test
    void BR_M05_01_skipsLevelWithNobody() {
        assertThat(AlertEscalationChain.next(0, level -> level == 2)).hasValue(2);
    }

    @Test
    void BR_M05_01_maximumLevelHasNoNext() {
        assertThat(AlertEscalationChain.next(2, level -> true)).isEmpty();
    }

    @Test
    void BR_M05_01_noRecipientsAnywhereHasNoNext() {
        assertThat(AlertEscalationChain.next(0, level -> false)).isEmpty();
    }

    @Test
    void BR_M05_01_reachedMaximumOnlyAtLevelTwo() {
        assertThat(AlertEscalationChain.atMaximum(1)).isFalse();
        assertThat(AlertEscalationChain.atMaximum(2)).isTrue();
    }
}
