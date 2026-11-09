package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodStatus;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFood;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** Đồ ăn gia đình (api-011 {@code FamilyFood} = {@code FamilyFoodCommand} + {@code id}, {@code status}, {@code systemReasons}). */
public record FamilyFoodResponse(
        long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String senderName,
        Long familyMemberId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String description,
        List<Long> ingredients,
        boolean ingredientsUnknown,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant expiresAt,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String storageMethod,
        FamilyFoodStatus status,
        List<String> systemReasons) {

    public static FamilyFoodResponse of(SimpleRecord<FamilyFood> row) {
        FamilyFood f = row.content();
        return new FamilyFoodResponse(
                row.id(),
                f.senderName(),
                f.familyMemberId(),
                f.type(),
                f.description(),
                f.ingredients(),
                f.ingredientsUnknown(),
                f.expiresAt(),
                f.storageMethod(),
                FamilyFoodStatus.valueOf(row.status()),
                f.systemReasons());
    }
}
