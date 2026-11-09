package com.eldercare.backend.nutrition.internal.assignment.domain;

import com.eldercare.backend.nutrition.api.dto.DietAssignmentStatus;
import com.eldercare.backend.nutrition.api.dto.Texture;
import java.time.Instant;
import java.util.List;

/** Bản gán chế độ ăn (bảng {@code diet_assignment}, data-model mục 4). */
public record DietAssignment(
        long id,
        long residentId,
        long dietId,
        Texture texture,
        List<Long> personalRestrictions,
        String personalInstruction,
        AssignmentBasis basis,
        Long basisId,
        Long reviewNoticeId,
        Instant requestedEffectiveFrom,
        Instant actualEffectiveAt,
        DietAssignmentStatus status,
        String professionalReason,
        int version) {

    /** Đề xuất, Chờ hiệu lực: bản đang chờ (FR-012). */
    public boolean waiting() {
        return status == DietAssignmentStatus.PROPOSED || status == DietAssignmentStatus.PENDING_EFFECTIVE;
    }
}
