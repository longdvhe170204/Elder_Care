package com.eldercare.backend.billing.balance.web.request;

import jakarta.validation.constraints.NotNull;

/** Thân lệnh đính kèm bản ký của bảng quyết toán (api-017 {@code POST /settlement-statements/{id}/signed-copy}). */
public record SignedCopyRequest(@NotNull Long fileId) {}
