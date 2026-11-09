package com.eldercare.backend.nutrition.internal.assignment.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.nutrition.internal.assignment.domain.DietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.persistence.DietAssignmentStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Gắn phạm vi cho bản gán ({@code GanPhamViProvider} {@code M08.DIET_ASSIGNMENT}; T007): người cao tuổi của bản gán. */
@Component
class DietAssignmentScopeAssignment implements ScopeAssignmentProvider {

    private final DietAssignmentStore store;

    DietAssignmentScopeAssignment(DietAssignmentStore store) {
        this.store = store;
    }

    @Override
    public String entityType() {
        return DietAssignmentService.ENTITY;
    }

    @Override
    @Transactional(readOnly = true)
    public Target assignScope(long id) {
        DietAssignment assignment = store.find(id).orElseThrow(NotFoundException::new);
        return DietAssignmentService.target(assignment.residentId(), assignment.id());
    }
}
