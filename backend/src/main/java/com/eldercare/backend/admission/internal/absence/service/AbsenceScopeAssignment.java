package com.eldercare.backend.admission.internal.absence.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.admission.internal.absence.domain.Absence;
import com.eldercare.backend.admission.internal.absence.persistence.AbsenceStore;
import com.eldercare.backend.common.error.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Gắn phạm vi cho lượt vắng (T011; 002 contracts mục 1.9): {@code Target} mang người cao tuổi của lượt. */
@Component
class AbsenceScopeAssignment implements ScopeAssignmentProvider {

    private final AbsenceStore store;

    AbsenceScopeAssignment(AbsenceStore store) {
        this.store = store;
    }

    @Override
    public String entityType() {
        return AbsenceService.ENTITY_TYPE;
    }

    @Override
    @Transactional(readOnly = true)
    public Target assignScope(long id) {
        Absence absence = store.find(id).orElseThrow(NotFoundException::new);
        return AbsenceService.target(absence.id(), absence.residentId());
    }
}
