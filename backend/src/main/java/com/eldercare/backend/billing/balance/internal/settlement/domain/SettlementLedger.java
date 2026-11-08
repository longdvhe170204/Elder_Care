package com.eldercare.backend.billing.balance.internal.settlement.domain;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.transaction.domain.BalanceEntry;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Số liệu quyết toán rút từ giao dịch của một hồ sơ (017 FR-022, FR-034; research E-09). Bảng kỳ cuối là giao dịch "Thanh
 * toán bảng chi phí" Đã xác nhận mới nhất (chưa có số liệu theo loại từ 010, plan "Ghi chú triển khai" Phase 8). Tiền theo
 * đồng.
 *
 * @param lastConfirmedId giao dịch Đã xác nhận mới nhất, rỗng khi chưa có (tên logic {@code giao_dich_cuoi_id})
 * @param balanceBeforeSettlement số dư ngay trước giao dịch thanh toán bảng kỳ cuối
 * @param sinceFinalPeriod giao dịch Đã xác nhận sau thanh toán bảng kỳ cuối, theo thứ tự
 */
public record SettlementLedger(
        int pending,
        Long lastConfirmedId,
        boolean hasNonZeroTransaction,
        long finalPeriodTotal,
        long balanceBeforeSettlement,
        long depositReceived,
        long depositOffset,
        long depositRefunded,
        List<BalanceEntry> sinceFinalPeriod,
        List<Long> confirmedIds) {

    public static SettlementLedger of(List<BalanceTransactionRow> rows) {
        List<BalanceTransactionRow> confirmed = rows.stream()
                .filter(r -> r.status() == TransactionStatus.CONFIRMED)
                .sorted(Comparator.comparingLong(BalanceTransactionRow::id))
                .toList();
        int pending = (int) rows.stream()
                .filter(r -> r.status() == TransactionStatus.PENDING_APPROVAL)
                .count();
        Optional<BalanceTransactionRow> finalPayment = confirmed.stream()
                .filter(r -> r.type() == TransactionType.STATEMENT_PAYMENT)
                .reduce((first, second) -> second);
        long finalPaymentId = finalPayment.map(BalanceTransactionRow::id).orElse(0L);
        return new SettlementLedger(
                pending,
                confirmed.isEmpty() ? null : confirmed.getLast().id(),
                confirmed.stream().anyMatch(r -> r.amount() != 0),
                finalPayment.map(r -> -r.amount()).orElse(0L),
                confirmed.stream()
                        .filter(r -> r.ledger() == Ledger.BALANCE && r.id() < finalPaymentId)
                        .mapToLong(BalanceTransactionRow::amount)
                        .sum(),
                deposit(confirmed, TransactionType.DEPOSIT_RECEIPT),
                -deposit(confirmed, TransactionType.DEPOSIT_OFFSET),
                -deposit(confirmed, TransactionType.DEPOSIT_REFUND),
                confirmed.stream()
                        .filter(r -> r.id() > finalPaymentId)
                        .map(SettlementLedger::entry)
                        .toList(),
                confirmed.stream().map(BalanceTransactionRow::id).toList());
    }

    /** Có giao dịch Đã xác nhận sau giao dịch {@code transactionId} (rỗng: có giao dịch Đã xác nhận nào không). */
    public boolean confirmedAfter(Long transactionId) {
        return confirmedIds.stream().anyMatch(id -> transactionId == null || id > transactionId);
    }

    private static long deposit(List<BalanceTransactionRow> confirmed, TransactionType type) {
        return confirmed.stream()
                .filter(r -> r.ledger() == Ledger.DEPOSIT && r.type() == type)
                .mapToLong(BalanceTransactionRow::amount)
                .sum();
    }

    private static BalanceEntry entry(BalanceTransactionRow r) {
        String reference = r.bankTransactionCode() != null
                ? r.bankTransactionCode()
                : r.costStatementId() == null ? null : r.costStatementId().toString();
        return new BalanceEntry(
                r.residentId(), r.ledger(), r.type(), r.amount(), r.confirmedAt(), r.status(), r.method(), reference);
    }
}
