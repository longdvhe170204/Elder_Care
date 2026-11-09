package com.eldercare.backend.billing.balance.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Thân chỉ có lý do (api-017 {@code requestBodies/Reason}) của lệnh dừng nhắc (FR-020). */
@Schema(name = "BalanceReasonBody")
public record StopRemindersRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", minLength = 1, maxLength = 2000)
        Reason reason) {}
