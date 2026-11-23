package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.notification.api.spi.SourceRecordAccessPort;
import org.springframework.stereotype.Component;

/**
 * Cài {@link SourceRecordAccessPort} của 009 cho đồ gửi (test luồng BF-14 2026-11-23): mở được khi người dùng xem được
 * đồ của người cao tuổi chủ đồ, như {@code GET /belongings/{id}}.
 */
@Component
public class BelongingSourceAccess implements SourceRecordAccessPort {

    private final BelongingStore belongings;
    private final BelongingQueries queries;

    public BelongingSourceAccess(BelongingStore belongings, BelongingQueries queries) {
        this.belongings = belongings;
        this.queries = queries;
    }

    @Override
    public String sourceRecordType() {
        return "M12.BELONGING";
    }

    @Override
    public boolean canOpen(String sourceRecordId) {
        long id;
        try {
            id = Long.parseLong(sourceRecordId);
        } catch (NumberFormatException e) {
            return false;
        }
        return belongings.find(id).map(b -> queries.canView(id, b.residentId())).orElse(false);
    }
}
