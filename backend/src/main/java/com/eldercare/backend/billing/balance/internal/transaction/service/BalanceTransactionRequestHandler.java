package com.eldercare.backend.billing.balance.internal.transaction.service;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import com.eldercare.backend.administration.approval.api.dto.ResultRef;
import com.eldercare.backend.administration.approval.api.events.RequestApplyFailed;
import com.eldercare.backend.administration.approval.api.events.RequestCancelled;
import com.eldercare.backend.administration.approval.api.events.RequestDecided;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.api.dto.Basis;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.domain.LedgerState;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceTransactionStore;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.ledger.service.BalanceQueries;
import com.eldercare.backend.billing.balance.internal.ledger.service.DepositStatuses;
import com.eldercare.backend.billing.balance.internal.ledger.service.LedgerPosting;
import com.eldercare.backend.billing.balance.internal.settlement.service.SettlementChanges;
import com.eldercare.backend.billing.balance.internal.status.domain.Trigger;
import com.eldercare.backend.billing.balance.internal.status.service.BalanceStatusService;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionChecks;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.billing.balance.internal.transaction.persistence.PendingTransactionStore;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Loại yêu cầu {@code M11.BALANCE_TRANSACTION} (017 T025; FR-013, FR-014; research E-06): Kế toán lập (cột T), Quản lý
 * viện duyệt (cột D, chú thích ³²). Lúc duyệt kiểm lại giới hạn số tiền và người nhận; không đạt thì 409, yêu cầu và
 * giao dịch giữ Chờ duyệt, Kế toán được báo ({@link BlockedApprovalNotices}). Áp dụng (ngay sau khi duyệt): giao dịch Đã
 * xác nhận, cập nhật sổ, giao dịch gốc "đã điều chỉnh"; từ chối, hủy thì đóng giao dịch.
 */
@Component
class BalanceTransactionRequestHandler implements RequestTypeHandler {

    static final AuditAction CONFIRMED = new AuditAction("BALANCE_TRANSACTION_CONFIRMED", false);
    static final AuditAction CLOSED = new AuditAction("BALANCE_TRANSACTION_CLOSED", false);

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final BalanceTransactionService service;
    private final LedgerPosting posting;
    private final LedgerStore ledgers;
    private final BalanceTransactionStore transactions;
    private final PendingTransactionStore pending;
    private final DepositStatuses deposits;
    private final BalanceStatusService statuses;
    private final BlockedApprovalNotices blocked;
    private final SettlementChanges settlement;
    private final ResidentApi residents;
    private final AuditLogApi audit;
    private final BusinessTime time;

    BalanceTransactionRequestHandler(
            BalanceTransactionService service,
            LedgerPosting posting,
            LedgerStore ledgers,
            BalanceTransactionStore transactions,
            PendingTransactionStore pending,
            DepositStatuses deposits,
            BalanceStatusService statuses,
            BlockedApprovalNotices blocked,
            SettlementChanges settlement,
            ResidentApi residents,
            AuditLogApi audit,
            BusinessTime time) {
        this.service = service;
        this.posting = posting;
        this.ledgers = ledgers;
        this.transactions = transactions;
        this.pending = pending;
        this.deposits = deposits;
        this.statuses = statuses;
        this.blocked = blocked;
        this.settlement = settlement;
        this.residents = residents;
        this.audit = audit;
        this.time = time;
    }

    @Override
    public String typeCode() {
        return BalanceTypes.TRANSACTION;
    }

    @Override
    public void validateContent(JsonNode content) {
        transactionId(content);
        residentId(content);
    }

    @Override
    public PermissionKey createPermission() {
        return new PermissionKey(Function.BALANCE_LEDGER, Operation.CREATE);
    }

    @Override
    public PermissionKey approvePermission() {
        return new PermissionKey(Function.BALANCE_LEDGER, Operation.APPROVE);
    }

    @Override
    public Target scope(JsonNode content) {
        return BalanceQueries.target(residentId(content));
    }

    @Override
    public List<UnmetCondition> checkOnApprove(ApprovalRequestView request) {
        BalanceTransactionRow row = row(request);
        List<UnmetCondition> unmet = unmet(row, ledgers.find(row.residentId()).orElseThrow());
        if (!unmet.isEmpty()) blocked.notifyAccountant(row, unmet);
        return unmet;
    }

    /** Kiểm lại dưới khóa sổ trong transaction áp dụng (thanh toán bảng có thể chen giữa lúc duyệt và lúc áp dụng). */
    @Override
    public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
        BalanceTransactionRow row = row(request);
        if (row.status() != TransactionStatus.PENDING_APPROVAL) {
            return List.of(new UnmetCondition("STATE_CHANGED", "Giao dịch không còn Chờ duyệt"));
        }
        return unmet(row, posting.lock(row.residentId()));
    }

    @Override
    public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
        BalanceTransactionRow row = row(request);
        posting.lock(row.residentId());
        pending.confirm(row.id(), request.decidedBy(), null, time.now());
        ledgers.add(row.residentId(), row.ledger(), row.amount());
        if (row.originalTransactionId() != null) pending.markAdjusted(row.originalTransactionId());
        audit.write(AuditLogEntry.builder()
                .action(CONFIRMED)
                .entity(BalanceTypes.TRANSACTION, row.id())
                .resident(row.residentId())
                .before(Map.of("status", TransactionStatus.PENDING_APPROVAL.name()))
                .after(Map.of("status", TransactionStatus.CONFIRMED.name(), "amount", row.amount()))
                .basis(new Basis(null, request.id(), null, null))
                .build());
        if (row.ledger() == Ledger.DEPOSIT) {
            deposits.recheck(row.residentId(), row.id(), row.createdBy());
        } else if (row.type() == TransactionType.ADJUSTMENT && statuses.eligible(row.residentId())) {
            // FR-018 (Q-268): điều chỉnh được duyệt thì tính lại tình trạng ngay; xấu đi thì báo như lần chuyển mới,
            // tốt lên không báo ngay (cùng quy tắc với lần tính hằng ngày)
            statuses.recalculate(row.residentId(), Trigger.DAILY, "TRANSACTION:" + row.id());
        }
        settlement.changed(row.residentId());
        return new ResultRef(BalanceTypes.TRANSACTION, Long.toString(row.id()));
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onDecided(RequestDecided event) {
        if (ApprovalStatus.REJECTED.equals(event.outcome())) close(event.request(), TransactionStatus.REJECTED);
    }

    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onCancelled(RequestCancelled event) {
        close(event.request(), TransactionStatus.CANCELLED);
    }

    /** Duyệt rồi mà lúc áp dụng không còn đạt: yêu cầu Áp dụng không thành, giao dịch Đã hủy (Kế toán lập lại). */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onApplyFailed(RequestApplyFailed event) {
        close(event.request(), TransactionStatus.CANCELLED);
    }

    private void close(ApprovalRequestView request, TransactionStatus status) {
        if (!BalanceTypes.TRANSACTION.equals(request.type())) return;
        long id = transactionId(request.content());
        pending.close(id, status, time.now());
        settlement.changed(residentId(request.content()));
        audit.write(AuditLogEntry.builder()
                .action(CLOSED)
                .entity(BalanceTypes.TRANSACTION, id)
                .resident(residentId(request.content()))
                .before(Map.of("status", TransactionStatus.PENDING_APPROVAL.name()))
                .after(Map.of("status", status.name()))
                .basis(new Basis(null, request.id(), null, null))
                .build());
    }

    private List<UnmetCondition> unmet(BalanceTransactionRow row, LedgerState ledger) {
        BalanceTransactionService.Recipient recipient = recipient(row.id());
        TransactionType originalType = row.originalTransactionId() == null
                ? null
                : transactions.find(row.originalTransactionId()).orElseThrow().type();
        return TransactionChecks.unmet(new TransactionChecks.Facts(
                row.type(),
                row.amount(),
                ledger.balance(),
                ledger.depositBalance(),
                service.unfinalizedTotal(row.residentId()),
                residents.isFinalStatus(row.residentId()),
                service.recipientIsRepresentative(row.residentId(), recipient),
                originalType));
    }

    private BalanceTransactionService.Recipient recipient(long id) {
        String json = pending.recipient(id);
        if (json == null) return null;
        JsonNode node = JSON.readTree(json);
        JsonNode member = node.get("familyMemberId");
        return new BalanceTransactionService.Recipient(member == null ? null : member.asLong(), null, null);
    }

    private BalanceTransactionRow row(ApprovalRequestView request) {
        return transactions.find(transactionId(request.content())).orElseThrow();
    }

    static long transactionId(JsonNode content) {
        return positive(content, "transactionId");
    }

    static long residentId(JsonNode content) {
        return positive(content, "residentId");
    }

    private static long positive(JsonNode content, String field) {
        JsonNode node = content == null ? null : content.get(field);
        if (node == null || !node.isIntegralNumber() || node.asLong() <= 0) {
            throw new InvalidValueException("Nội dung cần " + field);
        }
        return node.asLong();
    }
}
