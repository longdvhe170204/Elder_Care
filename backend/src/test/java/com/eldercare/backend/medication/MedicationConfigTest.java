package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.medication.internal.governance.config.MedicationConfig;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MedicationConfigTest {

    @Test
    void US8_medicationConfigDoesNotPublishLegacyJacksonMapper() {
        assertThat(Arrays.stream(MedicationConfig.class.getDeclaredMethods())
                        .map(method -> method.getReturnType().getName()))
                .doesNotContain("com.fasterxml.jackson.databind.ObjectMapper");
    }
}
