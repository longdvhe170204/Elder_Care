package com.eldercare.backend.billing.balance.internal.ledger.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionView;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.domain.ConfirmedEntry;
import com.eldercare.backend.billing.balance.internal.ledger.domain.Payments;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceTransactionStore;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kế toán ghi nhận tiền nộp (017 T014; FR-005 → FR-007, BR-M11-12, 16; research E-05): tiền mặt, chuyển khoản Đã xác nhận
 * ngay, không cấp số phiếu; nhiều dòng (khác sổ) cùng mã giao dịch ngân hàng trong một lệnh; mã đã có ở giao dịch khác
 * thì 409 kèm giao dịch đó, chỉ lưu khi Kế toán xác nhận lại (DBR-29, không chặn). Sau khi ghi: tính lại trạng thái đặt
 * cọc (FR-006) và tình trạng số dư (FR-018, Q-252, Q-257).
 */
@Service
public class PaymentService {

    static final AuditAction RECORDED = new AuditAction("BALANCE_PAYMENT_RECORDED", false);

    private final LedgerPosting posting;
    private final LedgerStore ledgers;
    private final BalanceTransactionStore transactions;
    private final DepositStatuses deposits;
    private final BalanceQueries queries;
    private final org.springframework.beans.factory.ObjectProvider<PaymentRecordedHook> hooks;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;

    public PaymentService(
            LedgerPosting posting,
            LedgerStore ledgers,
            BalanceTransactionStore transactions,
            DepositStatuses deposits,
            BalanceQueries queries,
            org.springframework.beans.factory.ObjectProvider<PaymentRecordedHook> hooks,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time) {
        this.posting = posting;
        this.ledgers = ledgers;
        this.transactions = transactions;
        this.deposits = deposits;
        this.queries = queries;
        this.hooks = hooks;
        this.audit = audit;
        this.user = user;
        this.time = time;
    }

    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Payments.Result recordCash(Target target, long residentId, Payments.Cash cash) {
        positive(cash.amount());
        posting.lock(residentId);
        var row = posting.postConfirmed(new ConfirmedEntry(
                residentId,
                cash.ledger(),
                cash.ledger() == Ledger.BALANCE ? TransactionType.CASH_PAYMENT : TransactionType.DEPOSIT_RECEIPT,
                cash.amount(),
                cash.receivedAt(),
                PaymentMethod.CASH,
                null,
                null,
                cash.payerName(),
                cash.note(),
                false,
                user.accountId()));
        return after(residentId, List.of(row));
    }

    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Payments.Result recordBankTransfer(Target target, long residentId, Payments.Transfer transfer) {
        if (transfer.lines().isEmpty()
                || transfer.lines().stream()
                                .map(Payments.TransferLine::ledger)
                                .distinct()
                                .count()
                        != transfer.lines().size()) {
            throw new InvalidValueException("Mỗi sổ một dòng; một lệnh có tối đa một dòng số dư và một dòng cọc");
        }
        transfer.lines().forEach(l -> positive(l.amount()));
        String code = transfer.bankTransactionCode().strip();
        posting.lock(residentId);
        List<BalanceTransactionRow> used = transactions.byBankCode(code, List.of());
        if (!used.isEmpty() && !transfer.confirmDuplicateCode()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "BANK_TRANSACTION_CODE_USED",
                    "Mã giao dịch ngân hàng đã có ở giao dịch khác; kiểm tra rồi xác nhận lại nếu đúng",
                    Map.of(
                            "duplicateTransactions",
                            used.stream().map(BalanceTransactionView::of).toList()));
        }
        var at = time.startOfDay(transfer.receivedOn());
        List<BalanceTransactionRow> rows = new ArrayList<>();
        for (Payments.TransferLine line : transfer.lines()) {
            rows.add(posting.postConfirmed(new ConfirmedEntry(
                    residentId,
                    line.ledger(),
                    line.ledger() == Ledger.BALANCE
                            ? TransactionType.BANK_TRANSFER_PAYMENT
                            : TransactionType.DEPOSIT_RECEIPT,
                    line.amount(),
                    at,
                    PaymentMethod.BANK_TRANSFER,
                    code,
                    null,
                    transfer.payerName(),
                    transfer.note(),
                    !used.isEmpty(),
                    user.accountId())));
        }
        return after(residentId, rows);
    }

    private Payments.Result after(long residentId, List<BalanceTransactionRow> rows) {
        for (BalanceTransactionRow row : rows) {
            var data = new LinkedHashMap<String, Object>();
            data.put("residentId", residentId);
            data.put("ledger", row.ledger().name());
            data.put("type", row.type().name());
            data.put("amount", row.amount());
            data.put("method", row.method().name());
            if (row.bankTransactionCode() != null) data.put("bankTransactionCode", row.bankTransactionCode());
            audit.write(AuditLogEntry.builder()
                    .action(RECORDED)
                    .entity(BalanceTypes.TRANSACTION, row.id())
                    .resident(residentId)
                    .after(data)
                    .build());
        }
        rows.stream()
                .filter(r -> r.ledger() == Ledger.DEPOSIT)
                .reduce((first, second) -> second)
                .ifPresent(r -> deposits.recheck(residentId, r.id(), r.createdBy()));
        hooks.orderedStream().forEach(h -> h.paymentRecorded(residentId, rows));
        var ledger = ledgers.find(residentId).orElseThrow();
        return new Payments.Result(rows.stream().map(BalanceTransactionView::of).toList(), queries.details(ledger));
    }

    private static void positive(long amount) {
        if (amount <= 0) throw new InvalidValueException("Số tiền phải lớn hơn 0");
    }

    /** Tình trạng số dư tính lại ngay sau giao dịch nộp tiền (FR-018, Q-252, Q-257); phần tình trạng cài. */
    public interface PaymentRecordedHook {
        void paymentRecorded(long residentId, List<BalanceTransactionRow> rows);
    }
}
