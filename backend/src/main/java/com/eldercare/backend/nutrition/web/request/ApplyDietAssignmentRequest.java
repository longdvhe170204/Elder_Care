package com.eldercare.backend.nutrition.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh Áp dụng bản gán (api-011 {@code /diet-assignments/{id}/apply}; FR-013). */
@Schema(name = "ApplyDietAssignment")
public record ApplyDietAssignmentRequest(String professionalReason) {}
