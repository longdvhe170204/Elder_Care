package com.eldercare.backend.billing.balance.internal.export.domain;

import com.eldercare.backend.billing.balance.internal.transaction.domain.BalanceEntry;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Báo cáo thu chi thu gọn theo khoảng ngày (017 FR-025 (b), Q-386; research E-11): tổng tiền gia đình nộp (nộp tiền mặt,
 * chuyển khoản, thu cọc), tổng đã trừ theo bảng chi phí (trị tuyệt đối của giao dịch thanh toán bảng; bảng tổng âm làm
 * giảm tổng), tổng tiền hoàn (hoàn tiền, hoàn cọc), và danh sách hồ sơ có số dư âm cuối khoảng kèm số còn nợ. Không có tổng
 * điều chỉnh, cấn trừ, không tách theo phương thức. Chỉ giao dịch Đã xác nhận trong khoảng.
 */
public record IncomeExpenseReport(long totalPaid, long totalDeducted, long totalRefunded, List<Debtor> debtors) {

    /** Hồ sơ còn nợ: {@code debt} là số dương = −số dư cuối khoảng. */
    public record Debtor(long residentId, long debt) {}

    private static final Set<TransactionType> PAID = Set.of(
            TransactionType.CASH_PAYMENT, TransactionType.BANK_TRANSFER_PAYMENT, TransactionType.DEPOSIT_RECEIPT);
    private static final Set<TransactionType> REFUNDED = Set.of(TransactionType.REFUND, TransactionType.DEPOSIT_REFUND);

    /**
     * @param entries giao dịch của mọi hồ sơ (lọc lại theo khoảng và trạng thái ở đây)
     * @param balanceAtEnd số dư (sổ số dư) của từng hồ sơ tại hết ngày {@code to}
     */
    public static IncomeExpenseReport of(
            List<BalanceEntry> entries, Map<Long, Long> balanceAtEnd, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(BusinessTime.ZONE).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant();
        List<BalanceEntry> inRange = entries.stream()
                .filter(entry -> entry.status() == TransactionStatus.CONFIRMED)
                .filter(entry -> !entry.at().isBefore(start) && entry.at().isBefore(end))
                .toList();
        List<Debtor> debtors = balanceAtEnd.entrySet().stream()
                .filter(balance -> balance.getValue() < 0)
                .map(balance -> new Debtor(balance.getKey(), -balance.getValue()))
                .sorted(Comparator.comparingLong(Debtor::debt).reversed().thenComparingLong(Debtor::residentId))
                .toList();
        return new IncomeExpenseReport(
                sum(inRange, PAID),
                -sum(inRange, Set.of(TransactionType.STATEMENT_PAYMENT)),
                -sum(inRange, REFUNDED),
                debtors);
    }

    private static long sum(List<BalanceEntry> entries, Set<TransactionType> types) {
        return entries.stream()
                .filter(entry -> types.contains(entry.type()))
                .mapToLong(BalanceEntry::amount)
                .sum();
    }
}
