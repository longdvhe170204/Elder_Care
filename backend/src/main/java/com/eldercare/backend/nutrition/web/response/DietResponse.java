package com.eldercare.backend.nutrition.web.response;

import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Chế độ ăn (api-011 {@code Diet}). */
@Schema(name = "Diet")
public record DietResponse(
        long id,
        String name,
        String description,
        boolean treatmentRelated,
        boolean isDefault,
        @Schema(description = "Id dị nguyên") List<Long> restrictions,
        Long safeDishId,
        CatalogStatus status,
        int version) {

    public static DietResponse of(Diet diet) {
        var a = diet.attributes();
        return new DietResponse(
                diet.id(),
                diet.name(),
                a.description(),
                a.treatmentRelated(),
                a.isDefault(),
                a.restrictions(),
                a.safeDishId(),
                diet.status(),
                diet.version());
    }
}
