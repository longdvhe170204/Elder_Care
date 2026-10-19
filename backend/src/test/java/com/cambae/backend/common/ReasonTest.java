package com.cambae.backend.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.error.ReasonRequiredException;
import org.junit.jupiter.api.Test;

/** FR-044, SC-008 (000 T017): lý do không rỗng, không chỉ có khoảng trắng, tối đa 2000 ký tự. */
class ReasonTest {

    @Test
    void FR_044_validReason_isKept() {
        assertThat(new Reason("Sai liều").value()).isEqualTo("Sai liều");
        assertThat(new Reason("x".repeat(2000)).value()).hasSize(2000);
    }

    @Test
    void SC_008_missingOrBlankReason_isRejected() {
        assertThatThrownBy(() -> new Reason(null)).isInstanceOf(ReasonRequiredException.class);
        assertThatThrownBy(() -> new Reason("")).isInstanceOf(ReasonRequiredException.class);
        assertThatThrownBy(() -> new Reason(" \t\n ")).isInstanceOf(ReasonRequiredException.class);
    }

    @Test
    void FR_044_reasonOver2000Chars_isRejected() {
        assertThatThrownBy(() -> new Reason("x".repeat(2001))).isInstanceOf(ReasonRequiredException.class);
    }
}
