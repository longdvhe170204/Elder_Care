package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** 005 FR-056: tên mục và giờ HH:mm của mốc sinh hoạt cố định. */
public record DailyRoutineWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        String name,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^[0-9]{2}:[0-9]{2}$")
        String time) {}
