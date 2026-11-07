package com.eldercare.backend.belongings.internal.returns.service;

import com.eldercare.backend.belongings.internal.returns.domain.RecipientConfirmation;
import org.springframework.stereotype.Component;

/**
 * Việc theo phút của xác nhận người nhận khác (FR-017b, FR-032; 013 T018): Hết hạn theo CFG-M10-08 và báo Quản lý viện
 * khi đủ điều kiện duyệt thay. Job {@code belongings.scheduling.M12MinuteJob} (T006) chỉ gọi hai phương thức này. Mỗi
 * yêu cầu xử lý trong transaction riêng.
 */
@Component
public class RecipientConfirmationTimers {

    private final RecipientConfirmationService confirmations;

    RecipientConfirmationTimers(RecipientConfirmationService confirmations) {
        this.confirmations = confirmations;
    }

    /** Hiệu lực quá hạn chưa dùng → Hết hạn, báo người lập. Trả số yêu cầu đã chuyển Hết hạn. */
    public int expireDue() {
        int expired = 0;
        for (long id : confirmations.idsOf(RecipientConfirmation.EFFECTIVE)) {
            if (confirmations.expire(id)) {
                expired++;
            }
        }
        return expired;
    }

    /** Chờ xác nhận đủ điều kiện duyệt thay → báo Quản lý viện (một lần mỗi yêu cầu). Trả số yêu cầu đã xét. */
    public int notifyEligible() {
        int checked = 0;
        for (long id : confirmations.idsOf(RecipientConfirmation.PENDING_CONFIRMATION)) {
            confirmations.notifyEligible(id);
            checked++;
        }
        return checked;
    }
}
