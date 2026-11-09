package com.eldercare.backend.billing.balance.internal.ledger.domain;

import com.eldercare.backend.admission.api.dto.DepositStatus;
import com.eldercare.backend.billing.api.dto.BalanceStatus;
import java.util.List;

/**
 * Sổ theo schema {@code Balance} của api-017 (FR-002 → FR-004, FR-006): {@code debt} khi số dư âm; người xem là Hành
 * chính thì các trường ngoài số dư, số dư cọc, trạng thái đặt cọc để rỗng và liệt kê ở {@code hiddenFields}.
 */
public record BalanceDetails(
        long residentId,
        String paymentCode,
        long balance,
        long depositBalance,
        Long debt,
        BalanceStatus status,
        Integer daysCovered,
        Long unfinalizedTotal,
        DepositStatus depositStatus,
        Boolean remindersStopped,
        PaymentInstructions paymentInstructions,
        List<String> hiddenFields) {

    /** Trường Hành chính không xem (E-12: Hành chính chỉ thấy số dư, số dư cọc, trạng thái đặt cọc). */
    public static final List<String> OFFICE_HIDDEN = List.of(
            "paymentCode",
            "debt",
            "status",
            "daysCovered",
            "unfinalizedTotal",
            "remindersStopped",
            "paymentInstructions");

    public BalanceDetails forOfficeStaff() {
        return new BalanceDetails(
                residentId,
                null,
                balance,
                depositBalance,
                null,
                null,
                null,
                null,
                depositStatus,
                null,
                null,
                OFFICE_HIDDEN);
    }
}
