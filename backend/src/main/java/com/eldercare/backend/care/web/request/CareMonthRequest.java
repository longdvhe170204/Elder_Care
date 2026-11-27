package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Q-453: tầng và tháng (yyyy-MM) của lệnh lập, công bố công việc tháng. */
@Schema(name = "CareMonthRequest")
public record CareMonthRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long floorAreaId,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-12")
        String month) {}
