package com.eldercare.backend.belongings.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân thêm, sửa, ngừng mức tình trạng (api-013 {@code /belongings/condition-levels}); có {@code id} là sửa. */
public record ConditionLevelRequest(
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int order,
        Boolean unusable,
        String status) {}
