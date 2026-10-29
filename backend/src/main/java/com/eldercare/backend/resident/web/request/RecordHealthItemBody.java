package com.eldercare.backend.resident.web.request;

import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Thân lệnh ghi nhận mục sức khỏe (FR-018 → FR-021). */
public record RecordHealthItemBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        HealthItemType type,

        Long allergenId,

        @Size(max = 1000) String freeText,

        @Schema(description = "Bắt buộc với ALLERGY") Long severityId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long sourceId,

        @Schema(defaultValue = "false") Boolean confirmDuplicate) {

    public RecordCommand command() {
        return new RecordCommand(
                type, allergenId, freeText, severityId, sourceId, Boolean.TRUE.equals(confirmDuplicate));
    }
}
