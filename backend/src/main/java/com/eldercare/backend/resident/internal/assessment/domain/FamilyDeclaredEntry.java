package com.eldercare.backend.resident.internal.assessment.domain;

import java.time.Instant;

/**
 * Một mục "Gia đình khai" lưu ở cột JSON {@code assessment.family_declared} (Q-412, FR-025a; data-model 0.1): chủ đề, nội
 * dung, nguồn theo mẫu nguồn ý kiến (chỉ người cao tuổi hoặc người thân), người ghi, thời điểm ghi.
 */
public record FamilyDeclaredEntry(
        FamilyDeclaredTopic topic,
        String content,
        String sourceType,
        Long sourceFamilyMemberId,
        long recordedBy,
        Instant recordedAt) {}
