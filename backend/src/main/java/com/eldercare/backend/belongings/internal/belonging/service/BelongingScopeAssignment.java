package com.eldercare.backend.belongings.internal.belonging.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.common.error.NotFoundException;
import org.springframework.stereotype.Component;

/** Phạm vi của đồ gửi là người cao tuổi chủ đồ ({@code GanPhamViProvider} {@code M12.DO_GUI}; 013 T007, FR-029). */
@Component
class BelongingScopeAssignment implements ScopeAssignmentProvider {

    static final String ENTITY_TYPE = "M12.BELONGING";

    private final BelongingStore reads;

    BelongingScopeAssignment(BelongingStore reads) {
        this.reads = reads;
    }

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public Target assignScope(long id) {
        long residentId = reads.residentOf(id).orElseThrow(NotFoundException::new);
        return new Target(ENTITY_TYPE, Long.toString(id), residentId, null);
    }
}
