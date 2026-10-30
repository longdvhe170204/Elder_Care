package com.cambae.backend.m04.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** T007a: thân tạo/sửa chỉ có tên, không nhận cờ cố định hoặc trạng thái từ client. */
public record NonPerformanceReasonWrite(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 200)
        String name) {}
