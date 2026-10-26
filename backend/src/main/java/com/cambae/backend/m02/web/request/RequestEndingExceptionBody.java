package com.cambae.backend.m02.web.request;

import com.cambae.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Thân Lập yêu cầu ngoại lệ điều kiện kết thúc (contracts/api-004.yaml, POST {@code /residency-endings/{id}/request-exception};
 * FR-064). {@code conditionCode} kiểm ở backend để mã ngoài danh sách (bàn giao, xác nhận quyết toán) trả 422.
 */
public record RequestEndingExceptionBody(
        @NotNull
        @Schema(
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {
                    "END_A_COSTS",
                    "END_B_BELONGINGS",
                    "END_C_STORED_MEDICATION",
                    "END_D_ALERTS_INCIDENTS",
                    "END_H_SETTLEMENT"
                })
        String conditionCode,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {}
