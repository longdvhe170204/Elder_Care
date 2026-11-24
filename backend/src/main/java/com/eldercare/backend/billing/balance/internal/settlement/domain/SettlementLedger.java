package com.eldercare.backend.billing.balance.internal.settlement.domain;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.transaction.domain.BalanceEntry;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.LongPredicate;

/**
 * Số liệu quyết toán rút từ giao dịch của một hồ sơ (017 FR-022, FR-034; research E-09). Bảng kỳ cuối là giao dịch "Thanh
 * toán bảng chi phí" Đã xác nhận mới nhất của bảng mang cờ kỳ cuối của 010; bảng bổ sung chốt sau đó nằm trong
 * {@code sinceFinalPeriod} (BF-10, sửa 2026-11-24: trước lấy thanh toán mới nhất nên bảng bổ sung bị in thành bảng kỳ
 * cuối). Không biết cờ kỳ cuối thì lấy thanh toán mới nhất. Tiền theo đồng.
 *
 * @param lastConfirmedId giao dịch Đã xác nhận mới nhất, rỗng khi chưa có (tên logic {@code giao_dich_cuoi_id})
 * @param balanceBeforeSettlement số dư ngay trước giao dịch thanh toán bảng kỳ cuối
 * @param sinceFinalPeriod giao dịch Đã xác nhận sau thanh toán bảng kỳ cuối, theo thứ tự
 * @param finalPeriodStatementId mã bảng chi phí của thanh toán bảng kỳ cuối, rỗng khi chưa có
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
        List<Long> confirmedIds,
        Long finalPeriodStatementId) {

    public static SettlementLedger of(List<BalanceTransactionRow> rows) {
        return of(rows, statementId -> true);
    }

    /** @param finalPeriodStatement bảng chi phí có phải bảng kỳ cuối không (cờ kỳ cuối của 010) */
    public static SettlementLedger of(List<BalanceTransactionRow> rows, LongPredicate finalPeriodStatement) {
        List<BalanceTransactionRow> confirmed = rows.stream()
                .filter(r -> r.status() == TransactionStatus.CONFIRMED)
                .sorted(Comparator.comparingLong(BalanceTransactionRow::id))
                .toList();
        int pending = (int) rows.stream()
                .filter(r -> r.status() == TransactionStatus.PENDING_APPROVAL)
                .count();
        List<BalanceTransactionRow> payments = confirmed.stream()
                .filter(r -> r.type() == TransactionType.STATEMENT_PAYMENT)
                .toList();
        Optional<BalanceTransactionRow> finalPayment = payments.stream()
                .filter(r -> r.costStatementId() != null && finalPeriodStatement.test(r.costStatementId()))
                .reduce((first, second) -> second)
                .or(() -> payments.stream().reduce((first, second) -> second));
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
                confirmed.stream().map(BalanceTransactionRow::id).toList(),
                finalPayment.map(BalanceTransactionRow::costStatementId).orElse(null));
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
