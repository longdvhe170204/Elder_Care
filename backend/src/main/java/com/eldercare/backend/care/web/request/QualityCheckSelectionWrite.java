package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** 014 T025: thân {@code POST /quality-checks/shifts/{shiftId}} của api-014 (FR-059). */
public record QualityCheckSelectionWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long floorAreaId,

        @ArraySchema(minItems = 1) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        List<Long> careTaskIds) {}
