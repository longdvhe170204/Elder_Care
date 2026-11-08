package com.eldercare.backend.billing.balance.internal.ledger.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceDetails;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTransactionView;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BalanceTypes;
import com.eldercare.backend.billing.balance.internal.ledger.domain.BankInfo;
import com.eldercare.backend.billing.balance.internal.ledger.domain.LedgerState;
import com.eldercare.backend.billing.balance.internal.ledger.domain.PaymentCode;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.BalanceTransactionStore;
import com.eldercare.backend.billing.balance.internal.ledger.persistence.LedgerStore;
import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.internal.provision.domain.CostForBalancePort;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.notification.FamilyRight;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đọc sổ, giao dịch (017 T010; FR-002 → FR-004, research E-04, E-12): nhân viên có khóa {@code BALANCE_LEDGER} Xem
 * (Kế toán, Quản lý viện thấy đủ; Hành chính chỉ số dư, cọc, đặt cọc), người thân qua cổng khi quyền xem chi phí đang
 * bật (chú thích ¹⁶, kiểm mỗi lần gọi). Tổng chi phí chưa chốt lấy từ 010; chưa có bản cài thì 0.
 */
@Service
public class BalanceQueries {

    private static final java.util.Set<Role> FULL_VIEW = java.util.Set.of(Role.ACCOUNTANT, Role.NURSING_HOME_MANAGER);

    private final LedgerStore ledgers;
    private final BalanceTransactionStore transactions;
    private final DepositStatuses deposits;
    private final CollectionAccounts accounts;
    private final ObjectProvider<CostForBalancePort> costs;
    private final ObjectProvider<FamilyRelationshipApi> relationships;
    private final CurrentUser user;
    private final BusinessTime time;

    public BalanceQueries(
            LedgerStore ledgers,
            BalanceTransactionStore transactions,
            DepositStatuses deposits,
            CollectionAccounts accounts,
            ObjectProvider<CostForBalancePort> costs,
            ObjectProvider<FamilyRelationshipApi> relationships,
            CurrentUser user,
            BusinessTime time) {
        this.ledgers = ledgers;
        this.transactions = transactions;
        this.deposits = deposits;
        this.accounts = accounts;
        this.costs = costs;
        this.relationships = relationships;
        this.user = user;
        this.time = time;
    }

    public static Target target(long residentId) {
        return new Target(BalanceTypes.LEDGER, Long.toString(residentId), residentId, null);
    }

    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public BalanceDetails balance(Target target, long residentId) {
        requireStaff();
        BalanceDetails details = details(require(residentId));
        return user.roles().stream().anyMatch(FULL_VIEW::contains) ? details : details.forOfficeStaff();
    }

    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<BalanceTransactionView> transactions(
            Target target, long residentId, Ledger ledger, LocalDate from, LocalDate to, TransactionStatus status) {
        requireStaff();
        require(residentId);
        return transactions
                .list(
                        residentId,
                        ledger,
                        from == null
                                ? null
                                : from.atStartOfDay(BusinessTime.ZONE).toInstant(),
                        to == null
                                ? null
                                : to.plusDays(1).atStartOfDay(BusinessTime.ZONE).toInstant(),
                        status)
                .stream()
                .map(BalanceTransactionView::of)
                .toList();
    }

    /** Tìm hồ sơ theo mã nộp tiền đọc từ nội dung chuyển khoản (FR-007): sai ký tự kiểm tra thì 404. */
    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.CREATE)
    @Transactional(readOnly = true)
    public BalanceDetails byPaymentCode(String typed) {
        return PaymentCode.normalize(typed)
                .flatMap(ledgers::byPaymentCode)
                .map(this::details)
                .orElseThrow(NotFoundException::new);
    }

    public record Portal(BalanceDetails balance, List<BalanceTransactionView> transactions) {}

    /** Cổng người thân (FR-003): số dư, cọc, giao dịch Đã xác nhận, thông tin nộp tiền; cần quyền xem chi phí đang bật. */
    @RequiresPermission(function = Function.BALANCE_LEDGER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Portal portal(Target target, long residentId) {
        long familyMember = user.familyMemberId()
                .filter(id -> user.type() == AccountType.FAMILY_MEMBER)
                .orElseThrow(() -> new PermissionDeniedException("Chỉ người thân dùng cổng người thân"));
        FamilyRelationshipApi api = relationships.getIfAvailable();
        if (api == null || !api.rightEnabled(familyMember, residentId, FamilyRight.VIEW_COSTS, time.now())) {
            throw new PermissionDeniedException("Người thân chưa có quyền xem chi phí của người cao tuổi này");
        }
        LedgerState ledger = require(residentId);
        List<BalanceTransactionView> confirmed =
                transactions.list(residentId, null, null, null, TransactionStatus.CONFIRMED).stream()
                        .map(BalanceTransactionView::of)
                        .toList();
        return new Portal(details(ledger), confirmed);
    }

    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<CatalogItem<BankInfo>> banks() {
        return accounts.activeBanks();
    }

    long unfinalizedTotal(long residentId) {
        CostForBalancePort port = costs.getIfAvailable();
        return port == null ? 0 : port.unfinalizedTotal(residentId);
    }

    public BalanceDetails details(LedgerState l) {
        return new BalanceDetails(
                l.residentId(),
                l.paymentCode(),
                l.balance(),
                l.depositBalance(),
                l.balance() < 0 ? -l.balance() : null,
                l.status(),
                l.daysCovered(),
                unfinalizedTotal(l.residentId()),
                deposits.current(l.residentId(), l.depositBalance()).orElse(null),
                l.remindersStopped(),
                accounts.instructions(l.paymentCode()).orElse(null),
                null);
    }

    private LedgerState require(long residentId) {
        return ledgers.find(residentId).orElseThrow(NotFoundException::new);
    }

    private void requireStaff() {
        if (user.type() != AccountType.STAFF) {
            throw new PermissionDeniedException("Màn số dư của nhân viên; người thân xem ở cổng người thân");
        }
    }
}
