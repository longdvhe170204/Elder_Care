package com.eldercare.backend.nutrition.web.request;

import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.internal.assignment.domain.AssignmentBasis;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Thân lập đề xuất bản gán (api-011 {@code DietAssignmentCommand}; FR-010). */
@Schema(name = "DietAssignmentCommand")
public record DietAssignmentRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long dietId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Texture texture,
        List<Long> personalRestrictions,
        String personalInstruction,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) AssignmentBasis basis,
        Long basisId,
        Long reviewNoticeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant effectiveFrom) {}
