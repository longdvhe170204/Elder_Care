package com.cambae.backend.m04.internal.governance.service;

import com.cambae.backend.m03.api.ResidentLocationApi;
import com.cambae.backend.m04.internal.governance.persistence.CareGovernanceStore;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import org.springframework.beans.factory.ObjectProvider;

public record CareScopeAssignment(
        String entityType, CareGovernanceStore store, ObjectProvider<ResidentLocationApi> locations)
        implements ScopeAssignmentProvider {
    @Override
    public Target assignScope(long id) {
        Target result = store.scope(entityType, id);
        if (result.floorAreaId() != null || result.residentId() == null) return result;
        var source = locations.getIfAvailable();
        Long floor = source == null
                ? null
                : source.current(result.residentId())
                        .map(p -> p.floorId() != null ? p.floorId() : p.areaId())
                        .orElse(null);
        return new Target(result.type(), result.id(), result.residentId(), floor);
    }
}
