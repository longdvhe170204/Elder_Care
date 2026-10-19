package com.cambae.backend.common.error.web;

import com.cambae.backend.common.error.UnmetCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Mô tả schema {@code Problem} của hợp đồng cho OpenAPI sinh ra (research R-03); thân lỗi thật do
 * {@link ProblemDetailsHandler} dựng. Controller khai {@code @Schema(implementation = ProblemBody.class)} cho phản hồi
 * lỗi.
 */
@Schema(name = "Problem")
public record ProblemBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uri")
        String type,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Integer status,
        String detail,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String errorCode,
        List<UnmetCondition> unmetConditions,
        Object currentValue) {}
