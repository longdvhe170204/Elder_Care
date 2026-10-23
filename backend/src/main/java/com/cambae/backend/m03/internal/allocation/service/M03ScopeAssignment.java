package com.cambae.backend.m03.internal.allocation.service;

import com.cambae.backend.m03.internal.allocation.persistence.M03GovernanceStore;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;

/** T010: gắn phạm vi từ dữ liệu M03, không suy phạm vi toàn viện khi dữ liệu nguồn thiếu. */
public record M03ScopeAssignment(String entityType, M03GovernanceStore store) implements ScopeAssignmentProvider {
    @Override
    public Target assignScope(long id) {
        return store.scope(entityType, id);
    }
}
