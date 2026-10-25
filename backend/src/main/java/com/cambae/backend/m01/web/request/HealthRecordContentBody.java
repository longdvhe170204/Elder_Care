package com.cambae.backend.m01.web.request;

import com.cambae.backend.m01.internal.health.domain.InitialHealthRecordContent;
import io.swagger.v3.oas.annotations.media.Schema;

/** Nội dung hồ sơ sức khỏe ban đầu (FR-017); mọi trường tùy chọn. */
@Schema(name = "HealthRecordContent")
public record HealthRecordContentBody(
        String bloodType,
        String mobility,
        String selfCareAbility,
        String cognition,
        String initialNeedsNote,
        String nutritionNeeds,
        String currentMedications,
        String rehabilitationNeeds) {

    public InitialHealthRecordContent content() {
        return new InitialHealthRecordContent(
                bloodType,
                mobility,
                selfCareAbility,
                cognition,
                initialNeedsNote,
                nutritionNeeds,
                currentMedications,
                rehabilitationNeeds);
    }
}
