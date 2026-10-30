package com.eldercare.backend.roombedallocation.internal.allocation.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.internal.allocation.domain.*;
import com.eldercare.backend.roombedallocation.internal.allocation.persistence.AllocationReadStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-037: kiểm quyền từng người; không lộ phân bổ hoặc dấu cho người chỉ xem được vị trí. */
@Service
@Transactional(readOnly = true)
public class ResidentLocationQueries {
    private final ResidentLocationApi locations;
    private final AllocationReadStore store;
    private final ResidentApi residents;
    private final PermissionApi permissions;
    private final ScopeApi scope;
    private final CurrentUser user;

    public ResidentLocationQueries(
            ResidentLocationApi locations,
            AllocationReadStore store,
            ResidentApi residents,
            PermissionApi permissions,
            ScopeApi scope,
            CurrentUser user) {
        this.locations = locations;
        this.store = store;
        this.residents = residents;
        this.permissions = permissions;
        this.scope = scope;
        this.user = user;
    }

    public ResidentLocationDetails location(long residentId) {
        var current = locations.current(residentId).orElse(null);
        var target = new Target(
                "M01.RESIDENT", Long.toString(residentId), residentId, current == null ? null : current.floorId());
        boolean full = permissions
                .hasPermission(user, new PermissionKey(Function.BED_ALLOCATION, Operation.VIEW), target)
                .allowed();
        if (!full && !scope.inScope(user, target)) throw new NotFoundException();
        residents.identity(residentId);
        if (!full) return new ResidentLocationDetails(current, null, null, null);
        var rows = store.list(residentId, null, null);
        var active = rows.stream()
                .filter(row -> row.status() == AllocationStatus.ACTIVE)
                .findFirst()
                .orElse(null);
        var future = rows.stream()
                .filter(row -> row.status() == AllocationStatus.FUTURE)
                .findFirst()
                .orElse(null);
        return new ResidentLocationDetails(current, active, future, store.withoutBedSince(residentId));
    }
}
