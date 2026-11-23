package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.notification.api.spi.SourceRecordAccessPort;
import org.springframework.stereotype.Component;

/**
 * Cài {@link SourceRecordAccessPort} của 009 cho nguồn "đồ gửi của người cao tuổi" (mã là mã người cao tuổi; thông báo
 * đồ còn ở tủ tầng cũ FR-028, test luồng BF-14 2026-11-23): mở được khi người dùng xem được đồ gửi của người đó, như
 * {@code GET /residents/{residentId}/belongings}.
 */
@Component
public class ResidentBelongingsSourceAccess implements SourceRecordAccessPort {

    public static final String SOURCE_TYPE = "M12.BELONGING_RESIDENT";

    private final BelongingQueries queries;

    public ResidentBelongingsSourceAccess(BelongingQueries queries) {
        this.queries = queries;
    }

    @Override
    public String sourceRecordType() {
        return SOURCE_TYPE;
    }

    @Override
    public boolean canOpen(String sourceRecordId) {
        try {
            return queries.canViewResident(Long.parseLong(sourceRecordId));
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
