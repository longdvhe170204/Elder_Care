package com.eldercare.backend.admission.web.request;

import jakarta.validation.constraints.NotNull;

/**
 * Thân Đưa vào danh sách chờ (contracts/api-004.yaml, POST {@code /waiting-list-entries}): lượt đăng ký Đang xử lý và các
 * tình huống đặc biệt chọn từ bảng CFG-M02-08 (FR-007).
 */
public record AddToWaitingListBody(@NotNull Long applicationId) {}
