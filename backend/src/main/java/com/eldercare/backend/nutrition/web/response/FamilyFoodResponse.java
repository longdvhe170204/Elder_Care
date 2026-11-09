package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodStatus;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFood;
import java.time.Instant;
import java.util.List;

/** Đồ ăn gia đình (api-011 {@code FamilyFood} = {@code FamilyFoodCommand} + {@code id}, {@code status}, {@code systemReasons}). */
public record FamilyFoodResponse(
        long id,
        String senderName,
        Long familyMemberId,
        String type,
        String description,
        List<Long> ingredients,
        boolean ingredientsUnknown,
        Instant expiresAt,
        String storageMethod,
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
