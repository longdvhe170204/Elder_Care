package com.eldercare.backend.billing.balance.internal.status.domain;

import com.eldercare.backend.billing.api.dto.BalanceStatus;

/**
 * Tình trạng số dư (tên logic {@code TinhTrang}; FR-016, FR-017, BR-M11-13; research E-07): Còn nợ khi số dư − tổng chi
 * phí chưa chốt &lt; 0; ngược lại số ngày còn đủ tiền = phần còn lại ÷ phí một ngày (làm tròn xuống), Sắp hết tiền khi
 * nhỏ hơn CFG-M11-04. Phí một ngày bằng 0 hoặc chưa biết thì chỉ xét Còn nợ.
 */
public final class BalanceStatusRule {

    private BalanceStatusRule() {}

    public static StatusComputation compute(long balance, long unfinalizedTotal, Long dailyFee, int thresholdDays) {
        long available = balance - unfinalizedTotal;
        if (available < 0) return new StatusComputation(BalanceStatus.IN_DEBT, null);
        if (dailyFee == null || dailyFee <= 0) return new StatusComputation(BalanceStatus.NORMAL, null);
        int days = (int) Math.min(Integer.MAX_VALUE, Math.floorDiv(available, dailyFee));
        return new StatusComputation(days < thresholdDays ? BalanceStatus.RUNNING_LOW : BalanceStatus.NORMAL, days);
    }
}
