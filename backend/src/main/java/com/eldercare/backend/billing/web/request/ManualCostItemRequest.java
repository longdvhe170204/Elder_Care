package com.eldercare.backend.billing.web.request;

import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(name = "ManualCostItem")
public record ManualCostItemRequest(
        @NotNull @Positive Long residentId,
        @NotBlank String description,
        @NotNull LocalDate incurredOn,
        @NotNull @Positive BigDecimal quantity,
        @NotBlank String unit,
        @NotNull @PositiveOrZero Long unitPrice,
        @NotNull @Valid Reason reason,
        List<@NotNull @Positive Long> fileIds) {}
