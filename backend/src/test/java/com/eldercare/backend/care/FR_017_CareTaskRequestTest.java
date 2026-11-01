package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.web.request.CareTaskWrite;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class FR_017_CareTaskRequestTest {
    @Test
    void FR_017_optionalSharedCanBeOmittedForAnAssignedTask() {
        var body =
                "{\"location\":{\"type\":\"FLOOR\",\"id\":3},\"taskTypeId\":9,\"basis\":{\"type\":\"SHIFT_START_CHECK\"},\"plannedAt\":\"2026-11-01T01:00:00Z\",\"importance\":\"NORMAL\",\"performerId\":22}";
        assertThatCode(() -> assertThat(JsonMapper.builder()
                                .build()
                                .readValue(body, CareTaskWrite.class)
                                .draft()
                                .shared())
                        .isFalse())
                .doesNotThrowAnyException();
    }
}
