package com.eldercare.backend.billing.balance.internal.transaction.domain;

import com.eldercare.backend.common.error.UnmetCondition;
import java.util.ArrayList;
import java.util.List;

/**
 * Điều kiện của giao dịch cần duyệt (017 FR-011, FR-014; BR-M11-10, 14; research E-06), kiểm khi Kế toán lập và kiểm lại
 * khi Quản lý viện duyệt. Mã lỗi theo hợp đồng api-017 ({@code BlockingProblem}).
 */
public final class TransactionChecks {

    public static final String BALANCE_EXCEEDED = "BALANCE_EXCEEDED";
    public static final String DEPOSIT_BALANCE_EXCEEDED = "DEPOSIT_BALANCE_EXCEEDED";
    public static final String REFUND_LIMIT_EXCEEDED = "REFUND_LIMIT_EXCEEDED";
    public static final String RECIPIENT_NO_LONGER_REPRESENTATIVE = "RECIPIENT_NO_LONGER_REPRESENTATIVE";
    public static final String STATEMENT_PAYMENT_NOT_ADJUSTABLE = "STATEMENT_PAYMENT_NOT_ADJUSTABLE";

    /**
     * Số liệu tại lúc kiểm. {@code amount} là số tiền ghi vào sổ (hoàn: âm). {@code recipientIsRepresentative} rỗng khi
     * người nhận không phải người thân trong hệ thống (người nhận khác, hồ sơ không còn người đại diện).
     */
    public record Facts(
            TransactionType type,
            long amount,
            long balance,
            long depositBalance,
            long unfinalizedTotal,
            boolean finalStatus,
            Boolean recipientIsRepresentative,
            TransactionType originalType) {}

    private TransactionChecks() {}

    public static List<UnmetCondition> unmet(Facts f) {
        List<UnmetCondition> unmet = new ArrayList<>();
        switch (f.type()) {
            case REFUND -> {
                long refund = -f.amount();
                if (RefundLimit.check(RefundLimit.Kind.REFUND, f.balance(), 0, 0, true, refund)
                        != RefundLimit.Outcome.ALLOWED) {
                    unmet.add(new UnmetCondition(
                            BALANCE_EXCEEDED, "Số tiền hoàn vượt số dư hiện có", "BR-M11-14", "M11", false));
                } else if (RefundLimit.check(
                                RefundLimit.Kind.REFUND, f.balance(), 0, f.unfinalizedTotal(), f.finalStatus(), refund)
                        != RefundLimit.Outcome.ALLOWED) {
                    unmet.add(new UnmetCondition(
                            REFUND_LIMIT_EXCEEDED,
                            "Số tiền hoàn vượt số dư trừ tổng chi phí chưa chốt",
                            "BR-M11-14",
                            "M11",
                            false));
                }
            }
            case DEPOSIT_REFUND -> {
                if (RefundLimit.check(RefundLimit.Kind.DEPOSIT_REFUND, 0, f.depositBalance(), 0, true, -f.amount())
                        != RefundLimit.Outcome.ALLOWED) {
                    unmet.add(new UnmetCondition(
                            DEPOSIT_BALANCE_EXCEEDED,
                            "Số tiền hoàn cọc vượt số dư cọc hiện có",
                            "BR-M11-14",
                            "M11",
                            false));
                }
            }
            default -> {}
        }
        if (Boolean.FALSE.equals(f.recipientIsRepresentative())) {
            unmet.add(new UnmetCondition(
                    RECIPIENT_NO_LONGER_REPRESENTATIVE,
                    "Người nhận không còn là người đại diện Hiệu lực; lập lại với người nhận khác",
                    "BR-M11-14",
                    "M11",
                    false));
        }
        if (f.originalType() == TransactionType.STATEMENT_PAYMENT) {
            unmet.add(new UnmetCondition(
                    STATEMENT_PAYMENT_NOT_ADJUSTABLE,
                    "Không điều chỉnh giao dịch thanh toán bảng chi phí; sai sót của bảng đi qua khoản điều chỉnh của 010",
                    "DBR-30",
                    "M11",
                    false));
        }
        return unmet;
    }
}
