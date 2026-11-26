package com.eldercare.backend.billing.balance.internal.ledger.service;

import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.status.domain.TransactionNotices;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Báo người đại diện mỗi biến động số dư, tiền cọc (demo 2026-11-26, E làm, chờ chủ dự án duyệt; nội dung ở
 * {@link TransactionNotices}). Gọi trong transaction của lệnh, sau khi sổ đã cộng trừ: nộp tiền, thu cọc, cấn trừ cọc,
 * giao dịch được duyệt (hoàn số dư, hoàn cọc, điều chỉnh); giao dịch bị từ chối, hủy. Thanh toán bảng chi phí không báo
 * ở đây vì thông báo chốt bảng (010) đã nêu khoản đã trừ vào số dư.
 */
@Component
public class TransactionFamilyNotices {

    private final LedgerStore ledgers;
    private final ResidentApi residents;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    TransactionFamilyNotices(
            LedgerStore ledgers, ResidentApi residents, ApplicationEventPublisher events, BusinessTime time) {
        this.ledgers = ledgers;
        this.residents = residents;
        this.events = events;
        this.time = time;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void confirmed(long residentId, List<BalanceTransactionRow> rows) {
        List<TransactionNotices.Line> lines = rows.stream()
                .filter(r -> r.type() != TransactionType.STATEMENT_PAYMENT)
                .map(r -> new TransactionNotices.Line(r.type(), r.amount()))
                .toList();
        if (lines.isEmpty()) return;
        var ledger = ledgers.find(residentId).orElseThrow();
        String key = rows.stream().map(r -> Long.toString(r.id())).collect(Collectors.joining(","));
        events.publishEvent(TransactionNotices.confirmed(
                residentId, name(residentId), lines, ledger.balance(), ledger.depositBalance(), key, time.now()));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void closed(BalanceTransactionRow row, TransactionStatus status) {
        events.publishEvent(TransactionNotices.closed(
                row.residentId(),
                name(row.residentId()),
                row.type(),
                row.amount(),
                status == TransactionStatus.REJECTED,
                row.id() + ":" + status.name(),
                time.now()));
    }

    private String name(long residentId) {
        return residents.identity(residentId).fullName();
    }
}
