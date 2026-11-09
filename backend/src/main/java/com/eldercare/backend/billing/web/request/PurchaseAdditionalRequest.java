package com.eldercare.backend.billing.web.request;

import com.eldercare.backend.common.Reason;
import jakarta.validation.constraints.*;

public record PurchaseAdditionalRequest(
        @NotNull @Min(0) @Max(999999999999999L) Long newAmount,

        @NotNull
        @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class, minLength = 1, maxLength = 2000)
        Reason reason) {}
