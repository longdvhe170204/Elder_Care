package com.eldercare.backend.belongings.internal.returns.service;

import com.eldercare.backend.belongings.api.events.BelongingStatusChanged;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Nghe {@code BelongingStatusChanged} sau commit, transaction riêng (research K-11): đồ rời Đang giữ, Đang được sử dụng,
 * Hư hỏng thì bị loại khỏi danh sách yêu cầu xác nhận người nhận khác còn mở (FR-017b). Chỉ ủy quyền; test gọi thẳng
 * {@code RecipientConfirmationService.dropBelonging} vì listener chạy bất đồng bộ.
 */
@Component
class RecipientConfirmationListeners {

    private final RecipientConfirmationService confirmations;

    RecipientConfirmationListeners(RecipientConfirmationService confirmations) {
        this.confirmations = confirmations;
    }

    @ApplicationModuleListener
    void on(BelongingStatusChanged event) {
        confirmations.dropBelonging(event.belongingId());
    }
}
