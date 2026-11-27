package com.eldercare.backend.nutrition.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Kết quả lập đề xuất bản gán (api-011 {@code DietAssignmentCheck}): có cần lý do chuyên môn khi Áp dụng (FR-013). */
@Schema(name = "DietAssignmentCheck")
public record DietAssignmentCheckResponse(boolean professionalReasonRequired) {}
