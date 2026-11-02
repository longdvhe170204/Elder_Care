package com.eldercare.backend.admission.internal.ending.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.admission.internal.ending.domain.ResidencyEnding;
import com.eldercare.backend.admission.internal.ending.persistence.ResidencyEndingStore;
import com.eldercare.backend.common.error.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Gắn phạm vi cho hồ sơ kết thúc (T011; 002 contracts mục 1.9): {@code Target} mang người cao tuổi của hồ sơ. */
@Component
class ResidencyEndingScopeAssignment implements ScopeAssignmentProvider {

    private final ResidencyEndingStore store;

    ResidencyEndingScopeAssignment(ResidencyEndingStore store) {
        this.store = store;
    }

    @Override
    public String entityType() {
        return ResidencyEndingService.ENTITY_TYPE;
    }

    @Override
    @Transactional(readOnly = true)
    public Target assignScope(long id) {
        ResidencyEnding ending = store.find(id).orElseThrow(NotFoundException::new);
        return ResidencyEndingService.target(ending.id(), ending.residentId());
    }
}
