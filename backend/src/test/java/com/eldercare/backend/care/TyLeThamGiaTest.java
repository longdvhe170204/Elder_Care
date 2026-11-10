package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.internal.wellbeing.domain.ParticipationRate;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TyLeThamGiaTest {
    @Test
    void FR_070_onlyRefusedHealthAndOtherAbsenceEnterTheDenominator() {
        assertThat(ParticipationRate.calculate(
                        3, Map.of("REFUSED", 1, "HEALTH", 1, "OTHER", 1, "AWAY", 9, "NO_SHOW", 8)))
                .hasValue(0.5);
        assertThat(ParticipationRate.calculate(0, Map.of("AWAY", 9))).isEmpty();
        assertThat(ParticipationRate.calculate(1, Map.of())).hasValue(1.0);
    }
}
