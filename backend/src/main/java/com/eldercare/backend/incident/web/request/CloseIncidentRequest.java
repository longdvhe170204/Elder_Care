package com.eldercare.backend.incident.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh đóng sự cố: kết quả bắt buộc (BR-M05-09); sự cố lây nhiễm chọn kết quả (FR-066). */
public record CloseIncidentRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1)
        String resolution,

        @Schema(allowableValues = {"CONFIRMED_INFECTION", "RULED_OUT", "UNDETERMINED"})
        String infectionOutcome) {}
