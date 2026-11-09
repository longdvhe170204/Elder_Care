package com.eldercare.backend.nutrition.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;

/** Thân yêu cầu suất bổ sung (api-011 {@code POST /daily-meals/{id}/extra-portions}; FR-040). */
@Schema(name = "ExtraPortionCommand")
public record ExtraPortionRequestBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long residentId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long floorAreaId,

        @Schema(
                implementation = String.class,
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1,
                maxLength = 2000)
        Reason reason) {}
