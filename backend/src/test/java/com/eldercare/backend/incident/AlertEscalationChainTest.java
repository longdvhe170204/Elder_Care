package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.alert.domain.AlertEscalationChain;
import org.junit.jupiter.api.Test;

/** Chuỗi leo thang ba cấp (FR-033, research S-08): lên một cấp, bỏ qua cấp không có ai, cấp 2 là tối đa. */
class AlertEscalationChainTest {
    @Test
    void BR_40_levelZeroGoesToFloorManagerAndDoctors() {
        assertThat(AlertEscalationChain.next(0, level -> true)).hasValue(1);
    }

    @Test
    void BR_40_levelOneGoesToManager() {
        assertThat(AlertEscalationChain.next(1, level -> true)).hasValue(2);
    }
}
