package com.eldercare.backend.admission.internal.death.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.admission.internal.death.domain.DeathRecord;
import com.eldercare.backend.admission.internal.death.persistence.DeathStore;
import com.eldercare.backend.common.error.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Gắn phạm vi cho bản ghi qua đời (T011; 002 contracts mục 1.9): {@code Target} mang người cao tuổi của bản ghi. */
@Component
class DeathRecordScopeAssignment implements ScopeAssignmentProvider {

    private final DeathStore store;

    DeathRecordScopeAssignment(DeathStore store) {
        this.store = store;
    }

    @Override
    public String entityType() {
        return PostDeathTaskService.ENTITY_TYPE;
    }

    @Override
    @Transactional(readOnly = true)
    public Target assignScope(long id) {
        DeathRecord record = store.find(id).orElseThrow(NotFoundException::new);
        return DeathRecordService.target(record.id(), record.residentId());
    }
}
