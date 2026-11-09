package com.eldercare.backend.billing.balance.internal.ledger.domain;

import com.eldercare.backend.admission.api.dto.DepositStatus;
import java.util.OptionalLong;

/** Trạng thái đặt cọc (tên logic {@code TrangThaiDatCoc}; research E-05, FR-006). */
public final class DepositStatusRule {

    private DepositStatusRule() {}

    /** {@code requiredAmount} rỗng khi hợp đồng không yêu cầu cọc; đủ khoản cọc là số dư cọc không nhỏ hơn khoản cần. */
    public static DepositStatus of(OptionalLong requiredAmount, long depositBalance) {
        if (requiredAmount.isEmpty()) return DepositStatus.NOT_REQUIRED;
        return depositBalance >= requiredAmount.getAsLong() ? DepositStatus.SATISFIED : DepositStatus.NOT_SATISFIED;
    }
}
