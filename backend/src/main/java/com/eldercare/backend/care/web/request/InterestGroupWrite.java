package com.eldercare.backend.care.web.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** Thân lệnh tạo, đổi tên, ngừng nhóm sở thích (FR-051); không có {@code id} là tạo mới. */
public record InterestGroupWrite(
        Long id,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String name,
        String status) {}
