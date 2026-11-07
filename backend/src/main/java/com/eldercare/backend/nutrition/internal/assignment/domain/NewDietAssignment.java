package com.eldercare.backend.nutrition.internal.assignment.domain;

import com.eldercare.backend.nutrition.api.dto.Texture;
import java.time.Instant;
import java.util.List;

/** Nội dung bản gán do Bác sĩ lập (api-011 {@code DietAssignmentCommand}); không đổi sau khi lập (FR-010). */
public record NewDietAssignment(
        long dietId,
        Texture texture,
        List<Long> personalRestrictions,
        String personalInstruction,
        AssignmentBasis basis,
        Long basisId,
        Long reviewNoticeId,
        Instant effectiveFrom) {}
