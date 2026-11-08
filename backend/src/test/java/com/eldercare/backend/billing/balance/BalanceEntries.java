package com.eldercare.backend.billing.balance;

import com.eldercare.backend.billing.internal.balance.transaction.domain.BalanceEntry;
import com.eldercare.backend.billing.internal.balance.transaction.domain.Ledger;
import com.eldercare.backend.billing.internal.balance.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.internal.balance.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.internal.balance.transaction.domain.TransactionType;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** Dựng giao dịch mẫu cho test sao kê, báo cáo thu chi (017 T037); thời điểm theo giờ Việt Nam. */
final class BalanceEntries {

    private BalanceEntries() {}

    static Instant at(String date, String time) {
        return LocalDate.parse(date)
                .atTime(LocalTime.parse(time))
                .atZone(BusinessTime.ZONE)
                .toInstant();
    }

    static BalanceEntry entry(
            long residentId, Ledger ledger, TransactionType type, long amount, String date, String time) {
        return new BalanceEntry(
                residentId,
                ledger,
                type,
                amount,
                at(date, time),
                TransactionStatus.CONFIRMED,
                PaymentMethod.CASH,
                null);
    }

    static BalanceEntry withStatus(BalanceEntry entry, TransactionStatus status) {
        return new BalanceEntry(
                entry.residentId(),
                entry.ledger(),
                entry.type(),
                entry.amount(),
                entry.at(),
                status,
                entry.method(),
                entry.reference());
    }
}
