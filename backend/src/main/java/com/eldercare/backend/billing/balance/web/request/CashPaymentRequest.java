package com.eldercare.backend.billing.balance.web.request;

import com.eldercare.backend.billing.balance.internal.ledger.domain.Payments;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/** Thân {@code POST /residents/{residentId}/cash-payments} (FR-005): ghi chú ghi được số chứng từ của phần mềm kế toán. */
public record CashPaymentRequest(
        @NotNull Ledger ledger,
        @NotNull @Schema(minimum = "1") Long amount,
        @NotBlank String payerName,
        @NotNull Instant receivedAt,
        String note) {

    public Payments.Cash cash() {
        return new Payments.Cash(ledger, amount, payerName, receivedAt, note);
    }
}
