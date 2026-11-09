package com.eldercare.backend.billing.balance.internal.document.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.billing.api.dto.CostItemView;
import com.eldercare.backend.billing.api.dto.CostStatementType;
import com.eldercare.backend.billing.api.dto.CostStatementView;
import com.eldercare.backend.billing.balance.internal.document.domain.CostTypeLabels;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeData;
import com.eldercare.backend.billing.balance.internal.document.domain.PaymentNoticeRecords;
import com.eldercare.backend.billing.balance.internal.document.domain.StatementType;
import com.eldercare.backend.billing.balance.internal.document.persistence.PaymentNoticeStore;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceTransactionStore;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.ledger.service.CollectionAccounts;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.billing.internal.provision.domain.CostForBalancePort;
import com.eldercare.backend.billing.internal.provision.domain.FinalizedCostStatement;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh phát hành phiếu báo phí và danh sách chờ phát hành (017 T035, US9; FR-031, FR-032; BR-M11-21). Số liệu lấy từ bảng
 * Đã chốt của 010 ({@code CostForBalancePort.finalizedStatement}) và sổ; số dư trước, sau là số dư ngay trước và sau giao
 * dịch thanh toán bảng. Không tự tạo khi không có lệnh của Kế toán (Q-306).
 */
@Service
public class PaymentNoticeIssuance {

    /** Một bảng chờ phát hành cho màn hình (schema {@code StatementPendingNotice}). */
    public record Pending(
            long costStatementId,
            long residentId,
            String fullName,
            String period,
            String statementType,
            long total,
            Instant finalizedAt) {}

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("MM/yyyy");

    private final ObjectProvider<CostForBalancePort> costs;
    private final PaymentNoticeStore store;
    private final BalanceTransactionStore transactions;
    private final LedgerStore ledgers;
    private final CollectionAccounts accounts;
    private final PaymentNoticeService notices;
    private final ResidentApi residents;
    private final AccountApi accountNames;
    private final CurrentUser user;
    private final BusinessTime time;

    PaymentNoticeIssuance(
            ObjectProvider<CostForBalancePort> costs,
            PaymentNoticeStore store,
            BalanceTransactionStore transactions,
            LedgerStore ledgers,
            CollectionAccounts accounts,
            PaymentNoticeService notices,
            ResidentApi residents,
            AccountApi accountNames,
            CurrentUser user,
            BusinessTime time) {
        this.costs = costs;
        this.store = store;
        this.transactions = transactions;
        this.ledgers = ledgers;
        this.accounts = accounts;
        this.notices = notices;
        this.residents = residents;
        this.accountNames = accountNames;
        this.user = user;
        this.time = time;
    }

    /** FR-031: bảng Đã chốt đã có giao dịch thanh toán, chưa có phiếu; mới nhất trước. */
    @RequiresPermission(function = Function.PAYMENT_NOTICE, operation = Operation.CREATE)
    @Transactional(readOnly = true)
    public List<Pending> pendingIssue() {
        CostForBalancePort port = costs.getIfAvailable();
        if (port == null) return List.of();
        List<Pending> result = new ArrayList<>();
        for (var row : store.unissued(PaymentNoticeRecords.KIND.code() + ":")) {
            Optional<FinalizedCostStatement> statement = port.finalizedStatement(row.costStatementId());
            if (statement.isEmpty()) continue;
            CostStatementView s = statement.get().statement();
            result.add(new Pending(
                    row.costStatementId(),
                    row.residentId(),
                    residents.identity(row.residentId()).fullName(),
                    period(s),
                    type(s).name(),
                    s.total() == null ? 0 : s.total(),
                    row.paidAt()));
        }
        return result;
    }

    /** FR-031, FR-032: 404 khi không có bảng Đã chốt, 409 {@code STATEMENT_NOT_PAID} khi chưa có giao dịch thanh toán. */
    @RequiresPermission(function = Function.PAYMENT_NOTICE, operation = Operation.CREATE)
    @Transactional
    public PaymentNoticeService.Notice issue(long statementId) {
        CostForBalancePort port = costs.getIfAvailable();
        FinalizedCostStatement finalized =
                port == null ? null : port.finalizedStatement(statementId).orElse(null);
        if (finalized == null) throw new NotFoundException();
        CostStatementView statement = finalized.statement();
        long residentId = statement.residentId();
        List<BalanceTransactionRow> rows = transactions.list(residentId, null, null, null, null);
        BalanceTransactionRow payment = rows.stream()
                .filter(r -> r.type() == TransactionType.STATEMENT_PAYMENT
                        && r.status() == TransactionStatus.CONFIRMED
                        && Long.valueOf(statementId).equals(r.costStatementId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT,
                        "STATEMENT_NOT_PAID",
                        "Bảng chi phí chưa có giao dịch thanh toán trên sổ số dư"));
        long before = rows.stream()
                .filter(r -> r.ledger() == Ledger.BALANCE && r.status() == TransactionStatus.CONFIRMED)
                .filter(r -> precedes(r, payment))
                .mapToLong(BalanceTransactionRow::amount)
                .sum();
        var instructions = ledgers.find(residentId).flatMap(l -> accounts.instructions(l.paymentCode()));
        PaymentNoticeData data = new PaymentNoticeData(
                PaymentNoticeRecords.code(YearMonth.parse(statement.period()), statementId),
                time.now().atZone(BusinessTime.ZONE).toLocalDate(),
                accountNames.fullName(user.accountId()).orElse(""),
                residents.identity(residentId).fullName(),
                null,
                period(statement),
                type(statement),
                statement.items().stream().map(PaymentNoticeIssuance::line).toList(),
                statement.totalsByType().stream()
                        .map(t -> new PaymentNoticeData.TypeTotal(CostTypeLabels.of(t.type()), t.total()))
                        .toList(),
                -payment.amount(),
                before,
                before + payment.amount(),
                instructions
                        .map(i -> new PaymentNoticeData.PaymentInstructions(
                                i.bank(), i.accountNumber(), i.accountHolder(), i.paymentCode()))
                        .orElse(null));
        return notices.publish(
                BalanceQueries.target(residentId),
                residentId,
                statementId,
                data,
                instructions.map(i -> i.vietQr()).orElse(null));
    }

    /** Giao dịch {@code r} được xác nhận trước {@code payment} (cùng lúc thì theo mã). */
    private static boolean precedes(BalanceTransactionRow r, BalanceTransactionRow payment) {
        int byTime = r.confirmedAt().compareTo(payment.confirmedAt());
        return byTime < 0 || (byTime == 0 && r.id() < payment.id());
    }

    private static String period(CostStatementView s) {
        return YearMonth.parse(s.period()).format(PERIOD);
    }

    private static StatementType type(CostStatementView s) {
        if (s.finalPeriod()) return StatementType.FINAL_PERIOD;
        return s.statementType() == CostStatementType.SUPPLEMENTARY
                ? StatementType.SUPPLEMENTARY
                : StatementType.REGULAR;
    }

    private static PaymentNoticeData.Line line(CostItemView i) {
        long amount = i.amount() == null ? 0 : i.amount();
        return new PaymentNoticeData.Line(
                i.incurredOn(),
                CostTypeLabels.of(i.costType()),
                i.description(),
                i.quantity(),
                i.unitPrice() == null ? amount : i.unitPrice(),
                amount);
    }
}
