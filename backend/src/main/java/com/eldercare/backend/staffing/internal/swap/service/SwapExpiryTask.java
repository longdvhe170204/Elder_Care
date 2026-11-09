package com.eldercare.backend.staffing.internal.swap.service;

import com.eldercare.backend.staffing.internal.service.M09TimedTask;
import org.springframework.stereotype.Component;

/**
 * Khung job theo phút của 015 (research X-13): hết hiệu lực yêu cầu đổi ca, tách ca của yêu cầu nghỉ và nhắc theo CFG-M09-11. Chỉ chuyển trạng
 * thái và nhắc, không lập bản ghi nghiệp vụ (Q-306). Dựa trên trạng thái nên chạy bù an toàn.
 */
@Component
@org.springframework.core.annotation.Order(30)
class SwapExpiryTask implements M09TimedTask {

    private final SwapService swaps;
    private final LeaveService leaves;

    SwapExpiryTask(SwapService swaps, LeaveService leaves) {
        this.swaps = swaps;
        this.leaves = leaves;
    }

    @Override
    public Cadence cadence() {
        return Cadence.MINUTE;
    }

    @Override
    public int run(java.time.Instant scheduledAt, java.time.Instant actualAt) {
        return swaps.expireDue() + swaps.remindDue() + leaves.expireDue() + leaves.remindDue();
    }
}
