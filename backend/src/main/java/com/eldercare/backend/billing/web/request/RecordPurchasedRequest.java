package com.eldercare.backend.billing.web.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record RecordPurchasedRequest(
        @NotNull @Min(0) @Max(999999999999999L) Long actualAmount,
        @NotNull LocalDate purchasedOn,
        @NotNull @Positive Long receiptFileId,
        boolean urgentPurchase,
        @Positive Long urgentPurchaseRequesterId) {}
