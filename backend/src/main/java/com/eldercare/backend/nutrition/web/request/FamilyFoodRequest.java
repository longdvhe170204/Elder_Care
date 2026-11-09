package com.eldercare.backend.nutrition.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * Thân ghi nhận đồ ăn gia đình (api-011 {@code FamilyFoodCommand}; FR-052): {@code familyMemberId} khi người gửi chọn từ danh
 * sách người thân; {@code ingredients} là id dị nguyên, {@code ingredientsUnknown} khi chọn "không rõ".
 */
public record FamilyFoodRequest(
        @NotBlank String senderName,
        Long familyMemberId,
        @NotBlank String type,
        @NotBlank String description,
        List<Long> ingredients,
        boolean ingredientsUnknown,
        @NotNull Instant expiresAt,
        @NotBlank String storageMethod) {}
