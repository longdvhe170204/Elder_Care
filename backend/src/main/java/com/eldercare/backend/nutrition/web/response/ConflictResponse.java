package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictType;
import com.eldercare.backend.nutrition.internal.menu.service.ResidentMenuConflicts.Conflict;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** Xung đột của một món với một người (api-011 {@code Conflict}; FR-020, FR-022): chỉ trả cho người xem được dị ứng. */
@Schema(name = "Conflict")
public record ConflictResponse(
        LocalDate date,
        Long mealId,
        Long dishId,
        List<Long> ingredients,
        ConflictType type,
        Boolean hasSuitableSubstitute) {

    public static ConflictResponse of(Conflict conflict) {
        return new ConflictResponse(
                conflict.date(),
                conflict.mealId(),
                conflict.dishId(),
                conflict.ingredients(),
                conflict.type(),
                conflict.hasSuitableSubstitute());
    }
}
