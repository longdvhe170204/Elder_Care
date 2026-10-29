package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.M03GovernanceStore;

/** T010: gắn phạm vi từ dữ liệu M03, không suy phạm vi toàn viện khi dữ liệu nguồn thiếu. */
public record M03ScopeAssignment(String entityType, M03GovernanceStore store) implements ScopeAssignmentProvider {
    @Override
    public Target assignScope(long id) {
        return store.scope(entityType, id);
    }
}
