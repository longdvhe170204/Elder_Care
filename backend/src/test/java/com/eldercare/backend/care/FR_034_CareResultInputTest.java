package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.internal.result.domain.CareResultInput;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class FR_034_CareResultInputTest {
    @Test
    void FR_034_jsonNullIsPreservedForValidationInsteadOfCrashing() {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("occurred", null);
        var input = new CareResultInput(null, null, null, payload, false, null, null);
        assertThat(input.jsonValue()).containsEntry("occurred", null);
        payload.put("occurred", true);
        assertThat(input.jsonValue()).containsEntry("occurred", null);
        assertThatThrownBy(() -> input.jsonValue().put("other", false))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
