package com.eldercare.backend.admission.internal.contract.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.admission.internal.contract.domain.Contract;
import com.eldercare.backend.admission.internal.contract.persistence.ContractStore;
import com.eldercare.backend.common.error.NotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Gắn phạm vi cho hợp đồng (002 contracts mục 1.9): {@code Target} mang người cao tuổi của hợp đồng. */
@Component
class ContractScopeAssignment implements ScopeAssignmentProvider {

    private final ContractStore store;

    ContractScopeAssignment(ContractStore store) {
        this.store = store;
    }

    @Override
    public String entityType() {
        return ContractService.ENTITY_TYPE;
    }

    @Override
    @Transactional(readOnly = true)
    public Target assignScope(long id) {
        Contract contract = store.find(id).orElseThrow(NotFoundException::new);
        return ContractService.target(contract.id(), contract.residentId());
    }
}
