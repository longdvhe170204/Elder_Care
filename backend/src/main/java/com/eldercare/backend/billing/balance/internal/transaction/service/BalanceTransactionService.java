package com.eldercare.backend.billing.balance.internal.transaction.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.dto.CreateRequest;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.admission.api.ResidencyEndingReadApi;
import com.eldercare.backend.admission.api.spi.RepresentativePort;
import com.eldercare.backend.billing.api.FinalPeriodCostApi;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionRow;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionView;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.domain.LedgerState;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceTransactionStore;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.ledger.service.DepositStatuses;
import com.eldercare.backend.billing.balance.internal.ledger.service.LedgerPosting;
import com.eldercare.backend.billing.balance.internal.settlement.service.SettlementChanges;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionChecks;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import com.eldercare.backend.billing.balance.internal.transaction.persistence.PendingTransactionStore;
import com.eldercare.backend.billing.internal.provision.domain.CostForBalancePort;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Hoàn tiền, hoàn cọc, điều chỉnh Chờ duyệt (017 T025; FR-010, FR-011, FR-013, FR-014; BR-M11-10, 14; research E-06):
 * Kế toán lập, giao dịch ở Chờ duyệt và tạo yêu cầu {@code M11.BALANCE_TRANSACTION} của 000; sổ chỉ đổi khi Quản lý viện
 * duyệt ({@link BalanceTransactionRequestHandler}). Cấn trừ cọc (T026; FR-015) không duyệt, tạo cặp giao dịch liên kết.
 */
@Service
public class BalanceTransactionService {

    static final AuditAction REQUESTED = new AuditAction("BALANCE_TRANSACTION_REQUESTED", false);
    static final AuditAction OFFSET = new AuditAction("BALANCE_DEPOSIT_OFFSET", false);
    static final String OFFSET_CONDITIONS_NOT_MET = "OFFSET_CONDITIONS_NOT_MET";
    private static final Set<Role> PENDING_VIEWERS = Set.of(Role.ACCOUNTANT, Role.NURSING_HOME_MANAGER);

    /** Người nhận hoàn tiền (FR-014): người đại diện theo mã người thân, hoặc người nhận khác khi hồ sơ không còn người đại diện. */
    public record Recipient(Long familyMemberId, String fullName, String identityDocument) {}

    public record NewTransaction(
            TransactionType type,
            long residentId,
            Ledger ledger,
            long amount,
            Long originalTransactionId,
            Recipient recipient,
            PaymentMethod method,
            String note) {}

    public record Pending(BalanceTransactionView transaction, long residentId, String residentName) {}

    private final LedgerPosting posting;
    private final LedgerStore ledgers;
    private final BalanceTransactionStore transactions;
    private final PendingTransactionStore pending;
    private final DepositStatuses deposits;
    private final ObjectProvider<ApprovalRequestApi> approvals;
    private final ObjectProvider<RepresentativePort> representatives;
    private final ObjectProvider<CostForBalancePort> costs;
    private final ObjectProvider<ResidencyEndingReadApi> endings;
    private final ObjectProvider<FinalPeriodCostApi> finalPeriods;
    private final ResidentApi residents;
    private final SettlementChanges settlement;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;

    public BalanceTransactionService(
            LedgerPosting posting,
            LedgerStore ledgers,
            BalanceTransactionStore transactions,
            PendingTransactionStore pending,
            DepositStatuses deposits,
            ObjectProvider<ApprovalRequestApi> approvals,
            ObjectProvider<RepresentativePort> representatives,
            ObjectProvider<CostForBalancePort> costs,
            ObjectProvider<ResidencyEndingReadApi> endings,
            ObjectProvider<FinalPeriodCostApi> finalPeriods,
            ResidentApi residents,
            SettlementChanges settlement,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time) {
        this.posting = posting;
        this.ledgers = ledgers;
        this.transactions = transactions;
        this.pending = pending;
        this.deposits = deposits;
        this.approvals = approvals;
        this.representatives = representatives;
        this.costs = costs;
        this.endings = endings;
        this.finalPeriods = finalPeriods;
        this.residents = residents;
        this.settlement = settlement;
        this.audit = audit;
        this.user = user;
        this.time = time;
    }

    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = true)
    @Transactional
    public BalanceTransactionView create(Target target, NewTransaction command, Reason reason) {
        long residentId = command.residentId();
        TransactionType type = command.type();
        Ledger expected = switch (type) {
            case REFUND -> Ledger.BALANCE;
            case DEPOSIT_REFUND -> Ledger.DEPOSIT;
            case ADJUSTMENT -> command.ledger();
            default -> throw new InvalidValueException("Chỉ lập được hoàn tiền, hoàn cọc hoặc điều chỉnh");
        };
        if (command.ledger() != expected) {
            throw new InvalidValueException("Hoàn tiền ghi vào sổ số dư, hoàn cọc ghi vào sổ tiền cọc");
        }
        boolean refund = type != TransactionType.ADJUSTMENT;
        if (refund ? command.amount() <= 0 : command.amount() == 0) {
            throw new InvalidValueException(refund ? "Số tiền hoàn phải lớn hơn 0" : "Số tiền điều chỉnh phải khác 0");
        }
        LedgerState ledger = posting.lock(residentId);
        String recipient = null;
        PaymentMethod method = command.method() == null ? PaymentMethod.OTHER : command.method();
        if (refund) {
            recipient = recipientJson(residentId, command.recipient());
            if (command.method() == null || command.method() == PaymentMethod.INTERNAL) {
                throw new InvalidValueException("Hoàn tiền cần phương thức tiền mặt, chuyển khoản hoặc khác");
            }
        }
        BalanceTransactionRow original = null;
        if (type == TransactionType.ADJUSTMENT && command.originalTransactionId() != null) {
            original = transactions
                    .find(command.originalTransactionId())
                    .filter(t -> t.residentId() == residentId && t.status() == TransactionStatus.CONFIRMED)
                    .orElseThrow(() -> new InvalidValueException(
                            "Giao dịch được điều chỉnh phải là giao dịch Đã xác nhận của người cao tuổi này"));
            if (original.ledger() != command.ledger()) {
                throw new InvalidValueException("Điều chỉnh ghi vào cùng sổ với giao dịch được điều chỉnh");
            }
        }
        long amount = refund ? -command.amount() : command.amount();
        List<UnmetCondition> unmet = TransactionChecks.unmet(new TransactionChecks.Facts(
                type,
                amount,
                ledger.balance(),
                ledger.depositBalance(),
                unfinalizedTotal(residentId),
                residents.isFinalStatus(residentId),
                recipientIsRepresentative(residentId, command.recipient()),
                original == null ? null : original.type()));
        if (!unmet.isEmpty()) {
            UnmetCondition first = unmet.get(0);
            throw new BusinessException(
                    HttpStatus.CONFLICT, first.code(), first.description(), Map.of("unmetConditions", unmet));
        }
        long id = pending.insertPending(
                residentId,
                command.ledger(),
                type,
                amount,
                time.now(),
                method,
                original == null ? null : original.id(),
                recipient,
                command.note(),
                reason.value(),
                user.accountId());
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("transactionId", id);
        content.put("residentId", residentId);
        content.put("type", type.name());
        content.put("amount", amount);
        var request = approvals
                .getObject()
                .create(new CreateRequest(BalanceTypes.TRANSACTION, content, time.today(), reason, true));
        pending.attachRequest(id, request.id());
        // FR-022: giao dịch Chờ duyệt làm ý xác nhận quyết toán Chưa đạt
        settlement.changed(residentId);
        var after = new LinkedHashMap<String, Object>();
        after.put("type", type.name());
        after.put("ledger", command.ledger().name());
        after.put("amount", amount);
        after.put("status", TransactionStatus.PENDING_APPROVAL.name());
        after.put("approvalRequestId", request.id());
        audit.write(AuditLogEntry.builder()
                .action(REQUESTED)
                .entity(BalanceTypes.TRANSACTION, id)
                .resident(residentId)
                .after(after)
                .build());
        return BalanceTransactionView.of(transactions.find(id).orElseThrow());
    }

    /** Giao dịch Chờ duyệt của mọi hồ sơ (FR-013): Kế toán, Quản lý viện; Hành chính chỉ xem số dư (FR-028). */
    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Pending> pendingApproval() {
        if (user.type() != AccountType.STAFF || user.roles().stream().noneMatch(PENDING_VIEWERS::contains)) {
            throw new PermissionDeniedException("Chỉ Kế toán, Quản lý viện xem giao dịch chờ duyệt");
        }
        List<Pending> result = new ArrayList<>();
        for (long id : pending.pendingIds()) {
            BalanceTransactionRow row = transactions.find(id).orElseThrow();
            result.add(new Pending(
                    BalanceTransactionView.of(row),
                    row.residentId(),
                    residents.identity(row.residentId()).fullName()));
        }
        return result;
    }

    /**
     * Cấn trừ tiền cọc (FR-015): khi có hồ sơ kết thúc Đang chuẩn bị hoặc danh sách việc sau qua đời đang mở, và bảng kỳ
     * cuối đã chốt; số tiền không vượt số dư cọc và phần còn nợ. Hai giao dịch liên kết cùng lúc: sổ cọc giảm, số dư tăng.
     */
    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public List<BalanceTransactionView> offset(Target target, long residentId, long amount, String note) {
        if (amount <= 0) throw new InvalidValueException("Số tiền cấn trừ phải lớn hơn 0");
        LedgerState ledger = posting.lock(residentId);
        List<UnmetCondition> unmet = new ArrayList<>();
        ResidencyEndingReadApi ending = endings.getIfAvailable();
        if (ending == null || ending.settlementInfo(residentId).isEmpty()) {
            unmet.add(new UnmetCondition(
                    "ENDING_OR_POST_DEATH",
                    "Chưa có hồ sơ kết thúc lưu trú Đang chuẩn bị hoặc danh sách việc sau qua đời đang mở",
                    "FR-015",
                    "M02",
                    false));
        }
        FinalPeriodCostApi finalPeriod = finalPeriods.getIfAvailable();
        if (finalPeriod == null || !finalPeriod.finalPeriodCondition(residentId).satisfied()) {
            unmet.add(new UnmetCondition("FINAL_PERIOD_FINALIZED", "Bảng kỳ cuối chưa chốt", "FR-015", "M11", false));
        }
        if (amount > ledger.depositBalance()) {
            unmet.add(new UnmetCondition("DEPOSIT_BALANCE", "Vượt số dư cọc hiện có", "FR-015", "M11", false));
        }
        if (amount > Math.max(-ledger.balance(), 0)) {
            unmet.add(new UnmetCondition("DEBT", "Vượt phần còn nợ của số dư", "FR-015", "M11", false));
        }
        if (!unmet.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    OFFSET_CONDITIONS_NOT_MET,
                    "Chưa cấn trừ được tiền cọc",
                    Map.of("unmetConditions", unmet));
        }
        var now = time.now();
        long by = user.accountId();
        long depositSide = pending.insertOffset(residentId, Ledger.DEPOSIT, -amount, now, null, note, by);
        long balanceSide = pending.insertOffset(residentId, Ledger.BALANCE, amount, now, depositSide, note, by);
        pending.confirm(depositSide, null, balanceSide, now);
        ledgers.add(residentId, Ledger.DEPOSIT, -amount);
        ledgers.add(residentId, Ledger.BALANCE, amount);
        for (long id : List.of(depositSide, balanceSide)) {
            BalanceTransactionRow row = transactions.find(id).orElseThrow();
            audit.write(AuditLogEntry.builder()
                    .action(OFFSET)
                    .entity(BalanceTypes.TRANSACTION, id)
                    .resident(residentId)
                    .after(Map.of(
                            "ledger", row.ledger().name(),
                            "amount", row.amount(),
                            "linkedTransactionId", row.linkedTransactionId()))
                    .build());
        }
        deposits.recheck(residentId, depositSide, by);
        settlement.changed(residentId);
        return List.of(depositSide, balanceSide).stream()
                .map(id -> BalanceTransactionView.of(transactions.find(id).orElseThrow()))
                .toList();
    }

    long unfinalizedTotal(long residentId) {
        CostForBalancePort port = costs.getIfAvailable();
        return port == null ? 0 : port.unfinalizedTotal(residentId);
    }

    /** Rỗng khi người nhận không phải người thân trong hệ thống (người nhận khác). */
    Boolean recipientIsRepresentative(long residentId, Recipient recipient) {
        if (recipient == null || recipient.familyMemberId() == null) return null;
        RepresentativePort port = representatives.getIfAvailable();
        return port != null && port.isActiveRepresentative(recipient.familyMemberId(), residentId, time.today());
    }

    private String recipientJson(long residentId, Recipient recipient) {
        if (recipient == null || (recipient.familyMemberId() == null && blank(recipient.fullName()))) {
            throw new InvalidValueException("Hoàn tiền cần người nhận (FR-014)");
        }
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        if (recipient.familyMemberId() != null) {
            node.put("familyMemberId", recipient.familyMemberId());
        } else {
            RepresentativePort port = representatives.getIfAvailable();
            if (port != null && port.hasRepresentative(residentId)) {
                throw new InvalidValueException(
                        "Người cao tuổi còn người đại diện: người nhận phải là người đại diện Hiệu lực (FR-014)");
            }
            node.put("fullName", recipient.fullName().strip());
            if (!blank(recipient.identityDocument())) node.put("identityDocument", recipient.identityDocument());
        }
        return node.toString();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
