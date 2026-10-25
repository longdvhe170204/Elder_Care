package com.cambae.backend.m02.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Thân Đưa vào danh sách chờ (contracts/api-004.yaml, POST {@code /waiting-list-entries}): lượt đăng ký Đang xử lý và các
 * tình huống đặc biệt chọn từ bảng CFG-M02-08 (FR-007).
 */
public record AddToWaitingListBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long applicationId,

        @Schema(description = "Mã tình huống trong bảng CFG-M02-08")
        List<String> circumstances) {}
