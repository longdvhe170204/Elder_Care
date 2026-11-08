package com.eldercare.backend.billing.internal.balance.settlement.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Hai điều kiện kết thúc lưu trú do 017 trả cho 004 (FR-022, BR-M11-22; research E-08). {@code END_H_SETTLEMENT} gồm ba ý
 * về tiền (bảng kỳ cuối đã chốt và có giao dịch thanh toán, số dư cọc 0, số dư 0), miễn được bằng ngoại lệ đã duyệt của
 * 004. {@code END_H_SETTLEMENT_CONFIRMATION}: không còn giao dịch Chờ duyệt, có phiên bản bảng quyết toán hiện hành và
 * không có giao dịch Đã xác nhận nào sau lần xác nhận đó; không miễn được. Bảng bổ sung chưa chốt không làm Chưa đạt (Q-134).
 */
public final class SettlementConditions {

    public enum Code {
        END_H_SETTLEMENT,
        END_H_SETTLEMENT_CONFIRMATION
    }

    public enum Status {
        MET,
        NOT_MET
    }

    public enum Reason {
        FINAL_PERIOD_NOT_PAID,
        BALANCE_NOT_ZERO,
        DEPOSIT_NOT_ZERO,
        PENDING_TRANSACTIONS,
        NO_CURRENT_CONFIRMATION,
        TRANSACTION_AFTER_CONFIRMATION
    }

    /**
     * Số liệu đầu vào, tiền theo đồng.
     *
     * @param finalPeriodPaid bảng kỳ cuối đã chốt và đã có giao dịch thanh toán của bảng
     * @param pendingTransactions số giao dịch Chờ duyệt của hồ sơ
     * @param currentConfirmationExists có phiên bản bảng quyết toán hiện hành
     * @param transactionAfterConfirmation có giao dịch Đã xác nhận sau thời điểm xác nhận hiện hành
     */
    public record Input(
            boolean finalPeriodPaid,
            long balance,
            long depositBalance,
            int pendingTransactions,
            boolean currentConfirmationExists,
            boolean transactionAfterConfirmation) {}

    public record Condition(Code code, Status status, boolean waivable, List<Reason> reasons) {}

    public record Result(Condition settlement, Condition confirmation) {

        /** Cả hai điều kiện đều qua; ngoại lệ đã duyệt chỉ thay cho {@code END_H_SETTLEMENT}. */
        public boolean satisfied(boolean exceptionApproved) {
            boolean settlementOk = settlement.status() == Status.MET || (exceptionApproved && settlement.waivable());
            return settlementOk && confirmation.status() == Status.MET;
        }
    }

    private SettlementConditions() {}

    public static Result evaluate(Input input) {
        List<Reason> money = new ArrayList<>();
        if (!input.finalPeriodPaid()) {
            money.add(Reason.FINAL_PERIOD_NOT_PAID);
        }
        if (input.balance() != 0) {
            money.add(Reason.BALANCE_NOT_ZERO);
        }
        if (input.depositBalance() != 0) {
            money.add(Reason.DEPOSIT_NOT_ZERO);
        }
        List<Reason> confirmation = new ArrayList<>();
        if (input.pendingTransactions() > 0) {
            confirmation.add(Reason.PENDING_TRANSACTIONS);
        }
        if (!input.currentConfirmationExists()) {
            confirmation.add(Reason.NO_CURRENT_CONFIRMATION);
        } else if (input.transactionAfterConfirmation()) {
            confirmation.add(Reason.TRANSACTION_AFTER_CONFIRMATION);
        }
        return new Result(
                condition(Code.END_H_SETTLEMENT, true, money),
                condition(Code.END_H_SETTLEMENT_CONFIRMATION, false, confirmation));
    }

    private static Condition condition(Code code, boolean waivable, List<Reason> reasons) {
        return new Condition(code, reasons.isEmpty() ? Status.MET : Status.NOT_MET, waivable, List.copyOf(reasons));
    }
}
