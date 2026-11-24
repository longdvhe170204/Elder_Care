package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.notification.api.spi.SourceRecordAccessPort;
import org.springframework.stereotype.Component;

/**
 * Cài {@link SourceRecordAccessPort} của 009 cho bản ghi bàn giao (thất lạc, hư hỏng, hủy ghi nhận, bàn giao thay, đã trả;
 * test luồng BF-14 2026-11-23): mở được khi {@code GET /belongings/handovers/{id}} cho xem.
 */
@Component
public class HandoverSourceAccess implements SourceRecordAccessPort {

    private final BelongingQueries queries;

    public HandoverSourceAccess(BelongingQueries queries) {
        this.queries = queries;
    }

    @Override
    public String sourceRecordType() {
        return BelongingRecalculation.RECORD_TYPE;
    }

    @Override
    public boolean canOpen(String sourceRecordId) {
        try {
            queries.handoverRef(Long.parseLong(sourceRecordId));
            return true;
        } catch (NumberFormatException | BusinessException e) {
            return false;
        }
    }
}
