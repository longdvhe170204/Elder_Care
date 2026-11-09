package com.eldercare.backend.admission.internal.deposit.service;

import com.eldercare.backend.admission.api.events.DepositStatusChanged;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Nghe trạng thái đặt cọc của hợp đồng đổi (017 FR-006; 004 FR-030 → FR-032): kiểm lại điều kiện qua {@link
 * DepositRecheckService}, giá trị luôn đọc lại qua {@code DepositStatusPort}. Chạy sau khi 017 commit, giao dịch riêng.
 */
@Component
class DepositStatusListener {

    private final DepositRecheckService recheck;

    DepositStatusListener(DepositRecheckService recheck) {
        this.recheck = recheck;
    }

    @ApplicationModuleListener
    void onChanged(DepositStatusChanged event) {
        recheck.recheck(event.contractId());
    }
}
