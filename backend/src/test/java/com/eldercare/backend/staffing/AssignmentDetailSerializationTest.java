package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.staffing.web.response.AssignmentResponses;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class AssignmentDetailSerializationTest {
    @Test
    void FR_025_optionalNonNullableFieldsAreOmittedWhenAbsent() {
        var mapper = JsonMapper.builder().build();
        var at = Instant.parse("2026-11-07T15:00:00Z");
        var detail = new AssignmentResponses.Detail(
                9,
                "FLOOR",
                2,
                null,
                "CAREGIVER",
                "PRIMARY",
                false,
                at,
                at.plusSeconds(3600),
                22,
                "DRAFT",
                null,
                "MANUAL",
                12,
                0);
        var tree = mapper.valueToTree(detail);
        assertThat(tree.has("taskTypeId")).isFalse();
        assertThat(tree.has("actualEndAt")).isFalse();
        assertThat(tree.get("id").asLong()).isEqualTo(22);
        assertThat(tree.get("version").asLong()).isZero();
    }
}
