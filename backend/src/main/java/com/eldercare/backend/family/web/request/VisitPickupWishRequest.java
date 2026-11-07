package com.eldercare.backend.family.web.request;

import com.eldercare.backend.family.internal.right.domain.VisitPickupWish;
import com.eldercare.backend.family.internal.right.service.VisitPickupWishService.WishData;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Thân ghi mong muốn (FR-020a): người thân có quan hệ, hoặc người ngoài kèm họ tên và số giấy tờ. */
public record VisitPickupWishRequest(
        Long relationshipId,
        String fullName,
        String idDocumentNumber,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        VisitPickupWish.Type type,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean wanted,

        @NotNull @Valid @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        OpinionSource source) {

    public WishData data() {
        return new WishData(relationshipId, fullName, idDocumentNumber, type, wanted, source);
    }
}
