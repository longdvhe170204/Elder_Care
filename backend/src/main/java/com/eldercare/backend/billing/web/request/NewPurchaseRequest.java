package com.eldercare.backend.billing.web.request;

import com.eldercare.backend.common.Reason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record NewPurchaseRequest(
        @NotNull @Positive Long residentId,
        @Positive Long catalogItemId,
        String description,

        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 9, fraction = 3)
        BigDecimal quantity,

        @NotNull @Min(0) @Max(999999999999999L) Long estimatedAmount,

        @NotNull
        @io.swagger.v3.oas.annotations.media.Schema(implementation = String.class, minLength = 1, maxLength = 2000)
        Reason reason,

        @NotNull @Valid Requester requester) {
    public record Requester(
            @Pattern(regexp = "STAFF|FAMILY_MEMBER")
            @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"STAFF", "FAMILY_MEMBER"})
            String type,

            @Positive long id) {}
}
