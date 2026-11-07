package com.eldercare.backend.nutrition.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Thân lệnh của {@code /family-foods/{id}/{command}} (api-011): {@code note} cho {@code approve-use}, {@code reason} cho
 * {@code reject-use}, {@code method} (RETURNED, DISCARDED, CONSUMED) và {@code returnedTo} cho {@code record-disposal},
 * {@code end}.
 */
@Schema(name = "FamilyFoodCommandBody")
public record FamilyFoodCommandBody(
        String note,

        @Schema(implementation = String.class, maxLength = 2000)
        Reason reason,

        @Schema(allowableValues = {"RETURNED", "DISCARDED", "CONSUMED"})
        String method,

        String returnedTo) {}
