package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.notification.api.spi.SourceRecordAccessPort;
import org.springframework.stereotype.Component;

/**
 * Cài {@link SourceRecordAccessPort} của 009 cho nguồn "đồ có giá trị nhân viên đang giữ" (mã là mã nhân viên; nhắc còn
 * giữ đồ khi hết ca FR-014a, hoàn thiện BF-14 2026-11-23): mở được khi {@code GET /staff/{staffId}/held-belongings} cho
 * xem (chính nhân viên đó, Hành chính, Quản lý viện).
 */
@Component
public class StaffHeldSourceAccess implements SourceRecordAccessPort {

    public static final String SOURCE_TYPE = "M12.STAFF_HELD_BELONGINGS";

    private final BelongingQueries queries;

    public StaffHeldSourceAccess(BelongingQueries queries) {
        this.queries = queries;
    }

    @Override
    public String sourceRecordType() {
        return SOURCE_TYPE;
    }

    @Override
    public boolean canOpen(String sourceRecordId) {
        try {
            queries.heldValuableBy(Long.parseLong(sourceRecordId));
            return true;
        } catch (NumberFormatException | BusinessException e) {
            return false;
        }
    }
}
