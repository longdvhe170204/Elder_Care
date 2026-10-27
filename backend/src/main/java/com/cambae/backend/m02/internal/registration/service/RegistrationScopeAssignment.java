package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m02.internal.registration.persistence.ResidenceRegistrationStore;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Gắn phạm vi cho khai báo ({@code GanPhamViProvider}; 002 contracts mục 1.9): {@code Target} mang người cao tuổi. */
@Component
class RegistrationScopeAssignment implements ScopeAssignmentProvider {

    private final ResidenceRegistrationStore store;

    RegistrationScopeAssignment(ResidenceRegistrationStore store) {
        this.store = store;
    }

    @Override
    public String entityType() {
        return RegistrationNames.ENTITY_TYPE;
    }

    @Override
    @Transactional(readOnly = true)
    public Target assignScope(long id) {
        long residentId = store.residentOf(id).orElseThrow(NotFoundException::new);
        return RegistrationNames.target(id, residentId);
    }
}
