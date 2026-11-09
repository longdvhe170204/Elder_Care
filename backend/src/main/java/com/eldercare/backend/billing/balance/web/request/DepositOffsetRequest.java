package com.eldercare.backend.billing.balance.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Thân {@code POST /residents/{residentId}/deposit-offsets} (FR-015). */
public record DepositOffsetRequest(
        @NotNull @Schema(minimum = "1") Long amount, String note) {}
