package com.eldercare.backend.care.internal.governance.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.care.internal.governance.persistence.CareGovernanceStore;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
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
