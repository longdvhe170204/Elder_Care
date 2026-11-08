package com.eldercare.backend.billing.balance.internal.ledger.domain;

import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Dữ liệu lệnh ghi nhận tiền nộp (FR-005, FR-007; research E-05) và kết quả (schema {@code PaymentResult}). */
public final class Payments {

    private Payments() {}

    public record Cash(Ledger ledger, long amount, String payerName, Instant receivedAt, String note) {}

    public record TransferLine(Ledger ledger, long amount) {}

    public record Transfer(
            List<TransferLine> lines,
            LocalDate receivedOn,
            String bankTransactionCode,
            String payerName,
            String note,
            boolean confirmDuplicateCode) {}

    @Schema(name = "PaymentResult")
    public record Result(List<BalanceTransactionView> transactions, BalanceDetails balance) {}
}
