package com.eldercare.backend.belongings.internal.returns.service;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.notification.api.spi.SourceRecordAccessPort;
import org.springframework.stereotype.Component;

/**
 * Cài {@link SourceRecordAccessPort} của 009 cho yêu cầu xác nhận người nhận khác (FR-017, FR-030; plan 013 "Cập nhật
 * 2026-11-23"): người dùng hiện tại mở được khi API cổng xem yêu cầu ({@link RecipientConfirmationService#viewOnPortal})
 * cho xem, tức là tài khoản người thân và là người đại diện hiện tại của người cao tuổi.
 */
@Component
public class RecipientConfirmationSourceAccess implements SourceRecordAccessPort {

    static final String SOURCE_TYPE = "M12.RECIPIENT_CONFIRMATION";

    private final RecipientConfirmationService confirmations;

    public RecipientConfirmationSourceAccess(RecipientConfirmationService confirmations) {
        this.confirmations = confirmations;
    }

    @Override
    public String sourceRecordType() {
        return SOURCE_TYPE;
    }

    @Override
    public boolean canOpen(String sourceRecordId) {
        long id;
        try {
            id = Long.parseLong(sourceRecordId);
        } catch (NumberFormatException e) {
            return false;
        }
        try {
            confirmations.viewOnPortal(id);
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }
}
