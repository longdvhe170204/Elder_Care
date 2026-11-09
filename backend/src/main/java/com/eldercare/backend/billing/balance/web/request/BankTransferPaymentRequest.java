package com.eldercare.backend.billing.balance.web.request;

import com.eldercare.backend.billing.balance.internal.ledger.domain.Payments;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** Thân {@code POST /residents/{residentId}/bank-transfer-payments} (FR-007, DBR-29). */
public record BankTransferPaymentRequest(
        @NotNull @Size(min = 1, max = 2) List<@Valid Line> lines,
        @NotNull LocalDate receivedOn,
        @NotBlank String bankTransactionCode,
        String payerName,
        String note,
        @Schema(defaultValue = "false") Boolean confirmDuplicateCode) {

    @Schema(name = "BankTransferLine")
    public record Line(
            @NotNull Ledger ledger,
            @NotNull @Schema(minimum = "1") Long amount) {}

    public Payments.Transfer transfer() {
        return new Payments.Transfer(
                lines.stream()
                        .map(l -> new Payments.TransferLine(l.ledger(), l.amount()))
                        .toList(),
                receivedOn,
                bankTransactionCode,
                payerName,
                note,
                Boolean.TRUE.equals(confirmDuplicateCode));
    }
}
