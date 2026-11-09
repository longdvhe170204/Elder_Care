package com.eldercare.backend.billing.balance.internal.status.service;

import com.eldercare.backend.billing.internal.purchase.domain.PurchaseBalancePort;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Dấu "số dư không đủ" cho đề nghị mua hộ của 010 (tên logic {@code SoDuMuaHoPort}; FR-021, BR-M11-15): 010 hỏi lúc
 * hiển thị nên luôn theo số dư, tổng chưa chốt hiện tại; dấu không chặn. Chưa có sổ thì coi là không đủ.
 */
@Component
class PurchaseBalance implements PurchaseBalancePort {

    private final BalanceStatusService statuses;

    PurchaseBalance(BalanceStatusService statuses) {
        this.statuses = statuses;
    }

    @Override
    public boolean insufficientBalance(long residentId, long amount, Instant at) {
        return statuses.insufficient(residentId, amount);
    }
}
