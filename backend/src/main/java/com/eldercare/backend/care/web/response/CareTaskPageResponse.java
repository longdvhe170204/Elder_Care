package com.eldercare.backend.care.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Một trang lịch sử công việc đã lập (GET /care-tasks): các công việc của trang và tổng số khớp bộ lọc. */
@Schema(name = "CareTaskPage")
public record CareTaskPageResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<CareTaskResponse> items,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long total) {}
