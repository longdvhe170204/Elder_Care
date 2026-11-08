package com.eldercare.backend.billing.balance.web.request;

import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.billing.balance.internal.transaction.service.BalanceTransactionService;
import com.eldercare.backend.common.Reason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Thân {@code POST /balance-transactions} (schema {@code NewBalanceTransaction}; FR-010, FR-011, FR-014): hoàn tiền, hoàn
 * cọc ghi số dương (sổ ghi âm); điều chỉnh có dấu, khác 0.
 */
public record NewBalanceTransactionRequest(
        @NotNull @Schema(allowableValues = {"REFUND", "DEPOSIT_REFUND", "ADJUSTMENT"})
        TransactionType type,

        @NotNull Long residentId,
        @NotNull Ledger ledger,
        @NotNull Long amount,
        Long originalTransactionId,
        @Valid Recipient recipient,

        @Schema(allowableValues = {"CASH", "BANK_TRANSFER", "OTHER"})
        PaymentMethod method,

        @NotBlank @Size(max = 2000) String reason,
        String note) {

    /** Người nhận hoàn tiền (FR-014). */
    public record Recipient(Long familyMemberId, String fullName, String identityDocument) {}

    public BalanceTransactionService.NewTransaction command() {
        return new BalanceTransactionService.NewTransaction(
                type,
                residentId,
                ledger,
                amount,
                originalTransactionId,
                recipient == null
                        ? null
                        : new BalanceTransactionService.Recipient(
                                recipient.familyMemberId(), recipient.fullName(), recipient.identityDocument()),
                method,
                note);
    }

    public Reason reasonValue() {
        return new Reason(reason);
    }
}
