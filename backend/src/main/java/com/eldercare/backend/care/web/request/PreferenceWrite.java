package com.eldercare.backend.care.web.request;

import com.eldercare.backend.resident.api.dto.OpinionSource;
import io.swagger.v3.oas.annotations.media.Schema;

/** 014 T022: thân {@code POST /residents/{residentId}/preferences} của api-014 (FR-051, Q-363). */
public record PreferenceWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long interestGroupId,
        String description,

        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"LIKE", "DISLIKE"})
        String level,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) OpinionSource source,
        Boolean confirmReplacement) {}
