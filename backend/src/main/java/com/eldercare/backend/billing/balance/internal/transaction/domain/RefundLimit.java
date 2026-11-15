package com.eldercare.backend.billing.balance.internal.transaction.domain;

/**
 * Giới hạn số tiền hoàn tiền, hoàn cọc (017 FR-014, BR-M11-14; research E-06). Hoàn cọc không vượt số dư cọc; hoàn tiền
 * không vượt số dư, và khi hồ sơ chưa ở trạng thái cuối thì không vượt (số dư − tổng chi phí chưa chốt). Số tiền đơn vị
 * đồng. Dùng lúc lập và lúc duyệt (kiểm lại).
 */
public final class RefundLimit {

    public enum Kind {
        REFUND,
        DEPOSIT_REFUND
    }

    public enum Outcome {
        ALLOWED,
        INVALID_AMOUNT,
        EXCEEDED
    }

    private RefundLimit() {}

    /** Số tiền lớn nhất được hoàn, không âm. */
    public static long maximum(
            Kind kind, long balance, long depositBalance, long unfinalizedTotal, boolean finalStatus) {
        long limit = switch (kind) {
            case DEPOSIT_REFUND -> depositBalance;
            case REFUND -> finalStatus ? balance : balance - unfinalizedTotal;
        };
        return Math.max(limit, 0);
    }

    public static Outcome check(
            Kind kind, long balance, long depositBalance, long unfinalizedTotal, boolean finalStatus, long amount) {
        if (amount <= 0) {
            return Outcome.INVALID_AMOUNT;
        }
        return amount <= maximum(kind, balance, depositBalance, unfinalizedTotal, finalStatus)
                ? Outcome.ALLOWED
                : Outcome.EXCEEDED;
    }
}
