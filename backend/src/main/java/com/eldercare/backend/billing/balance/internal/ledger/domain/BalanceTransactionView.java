package com.eldercare.backend.billing.balance.internal.ledger.domain;

import com.eldercare.backend.billing.balance.internal.transaction.domain.Ledger;
import com.eldercare.backend.billing.balance.internal.transaction.domain.PaymentMethod;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionStatus;
import com.eldercare.backend.billing.balance.internal.transaction.domain.TransactionType;
import java.time.Instant;
import java.util.function.LongFunction;

/**
 * Giao dịch theo schema {@code BalanceTransaction} của api-017 (glossary 23.2): dùng cho phản hồi và cho thân lỗi
 * {@code DuplicateCodeProblem}. {@code createdBy}, {@code approvedBy} là họ tên của tài khoản (chủ dự án chọn
 * 2026-11-22, test BF-16); "Hệ thống" với giao dịch lập kèm lệnh chốt bảng (FR-012); tài khoản chưa có họ tên thì
 * "Tài khoản &lt;mã&gt;".
 */
public record BalanceTransactionView(
        long id,
        Ledger ledger,
        TransactionType type,
        long amount,
        Instant at,
        PaymentMethod method,
        String bankTransactionCode,
        Long costStatementId,
        Long originalTransactionId,
        Long linkedTransactionId,
        String createdBy,
        String approvedBy,
        String reason,
        String note,
        TransactionStatus status,
        boolean adjusted,
        Long approvalRequestId) {

    public static final String SYSTEM = "Hệ thống";

    /** {@code names} trả họ tên theo mã tài khoản, {@code null} khi chưa có. */
    public static BalanceTransactionView of(BalanceTransactionRow t, LongFunction<String> names) {
        return new BalanceTransactionView(
                t.id(),
                t.ledger(),
                t.type(),
                t.amount(),
                t.at(),
                t.method(),
                t.bankTransactionCode(),
                t.costStatementId(),
                t.originalTransactionId(),
                t.linkedTransactionId(),
                t.createdBy() == null ? SYSTEM : name(t.createdBy(), names),
                t.approvedBy() == null ? null : name(t.approvedBy(), names),
                t.reason(),
                t.note(),
                t.status(),
                t.adjusted(),
                t.approvalRequestId());
    }

    private static String name(long accountId, LongFunction<String> names) {
        String fullName = names.apply(accountId);
        return fullName == null ? "Tài khoản " + accountId : fullName;
    }
}
