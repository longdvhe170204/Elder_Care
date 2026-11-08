package com.eldercare.backend.billing.internal.balance.export.domain;

import com.eldercare.backend.billing.internal.balance.transaction.domain.BalanceEntry;
import com.eldercare.backend.billing.internal.balance.transaction.domain.Ledger;
import com.eldercare.backend.billing.internal.balance.transaction.domain.TransactionStatus;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Sao kê số dư của một người cao tuổi trong khoảng ngày (017 FR-025 (a), SC-009; research E-11): số dư và số dư cọc đầu
 * kỳ, các giao dịch Đã xác nhận trong khoảng, số dư và số dư cọc cuối kỳ, bảo đảm đầu kỳ + tổng giao dịch = cuối kỳ cho
 * từng sổ. Khoảng gồm cả hai đầu theo ngày giờ Việt Nam.
 */
public record BalanceStatement(
        long openingBalance, long openingDeposit, List<BalanceEntry> lines, long closingBalance, long closingDeposit) {

    /** {@code history} là các giao dịch của một người cao tuổi từ đầu tới ít nhất hết ngày {@code to}. */
    public static BalanceStatement of(List<BalanceEntry> history, LocalDate from, LocalDate to) {
        Instant start = from.atStartOfDay(BusinessTime.ZONE).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant();
        List<BalanceEntry> confirmed = history.stream()
                .filter(entry -> entry.status() == TransactionStatus.CONFIRMED)
                .toList();
        long openingBalance = sum(confirmed, Ledger.BALANCE, entry -> entry.at().isBefore(start));
        long openingDeposit = sum(confirmed, Ledger.DEPOSIT, entry -> entry.at().isBefore(start));
        List<BalanceEntry> lines = confirmed.stream()
                .filter(entry -> !entry.at().isBefore(start) && entry.at().isBefore(end))
                .sorted(Comparator.comparing(BalanceEntry::at))
                .toList();
        return new BalanceStatement(
                openingBalance,
                openingDeposit,
                lines,
                openingBalance + sum(lines, Ledger.BALANCE, entry -> true),
                openingDeposit + sum(lines, Ledger.DEPOSIT, entry -> true));
    }

    private static long sum(List<BalanceEntry> entries, Ledger ledger, Predicate<BalanceEntry> filter) {
        return entries.stream()
                .filter(entry -> entry.ledger() == ledger && filter.test(entry))
                .mapToLong(BalanceEntry::amount)
                .sum();
    }
}
