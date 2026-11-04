package com.eldercare.backend.billing.internal.governance.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.billing.internal.governance.persistence.CostScopeStore;
import com.eldercare.backend.common.error.NotFoundException;

/** T008: trả dữ liệu phạm vi, quyết định quyền vẫn do API 002 và lệnh nghiệp vụ giữ. */
public class CostScopeAssignment implements ScopeAssignmentProvider {
    private final String type;
    private final CostScopeStore store;

    public CostScopeAssignment(String type, CostScopeStore store) {
        this.type = type;
        this.store = store;
    }

    public String entityType() {
        return type;
    }

    public Target assignScope(long id) {
        long resident = store.resident(type, id).orElseThrow(NotFoundException::new);
        return new Target(type, Long.toString(id), resident, null);
    }
}
