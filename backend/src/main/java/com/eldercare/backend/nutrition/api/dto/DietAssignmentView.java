package com.eldercare.backend.nutrition.api.dto;

import java.time.Instant;
import java.util.Set;

/** Một bản gán chế độ ăn trong lịch sử của người cao tuổi (012 hiển thị khi có quyền xem sức khỏe). */
public record DietAssignmentView(
        long id,
        long residentId,
        long dietId,
        Texture texture,
        Set<Long> personalRestrictions,
        String personalInstruction,
        DietAssignmentStatus status,
        Instant effectiveFrom,
        Instant actualEffectiveAt) {}
