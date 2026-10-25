package com.cambae.backend.common.error.web;

import com.cambae.backend.common.error.UnmetCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
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
        Object currentValue,

        /** Bản ghi có thể trùng (001 {@code POSSIBLE_DUPLICATE_PROFILE}, {@code DUPLICATE_HEALTH_ITEM}). */
        List<Object> candidates,

        @Schema(allowableValues = {"ROLE", "SCOPE", "LEGAL", "SOURCE_DATA"})
        String failedLayer,

        License license,

        /** Khi 423 {@code ACCOUNT_TEMPORARILY_LOCKED}: thời điểm hết khóa tạm (002 FR-014). */
        java.time.Instant unlocksAt) {

    /** Giấy phép làm lớp pháp lý không thỏa (002 FR-043). */
    public record License(String name, LocalDate expiryDate) {}
}
