package com.eldercare.backend.admission.internal.waitinglist.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.admission.internal.waitinglist.domain.WaitingListEntry;
import com.eldercare.backend.admission.internal.waitinglist.persistence.WaitingListStore;
import com.eldercare.backend.common.error.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Gắn phạm vi cho hồ sơ chờ (002 contracts mục 1.9): {@code Target} mang người cao tuổi của hồ sơ. */
@Component
class WaitingListScopeAssignment implements ScopeAssignmentProvider {

    private final WaitingListStore store;

    WaitingListScopeAssignment(WaitingListStore store) {
        this.store = store;
    }

    @Override
    public String entityType() {
        return WaitingListService.ENTITY_TYPE;
    }

    @Override
    @Transactional(readOnly = true)
    public Target assignScope(long id) {
        WaitingListEntry entry = store.find(id).orElseThrow(NotFoundException::new);
        return WaitingListService.target(entry.id(), entry.residentId());
    }
}
