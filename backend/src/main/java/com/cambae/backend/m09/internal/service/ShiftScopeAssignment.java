package com.cambae.backend.m09.internal.service;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m09.shift.internal.persistence.ShiftStore;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;

/** T009/T012: phạm vi lấy từ lịch/ca thật; không suy phạm vi từ mã nhân viên. */
public class ShiftScopeAssignment implements ScopeAssignmentProvider {
    private final String type;
    private final ShiftStore store;

    public ShiftScopeAssignment(String type, ShiftStore store) {
        this.type = type;
        this.store = store;
    }

    public String entityType() {
        return type;
    }

    public Target assignScope(long id) {
        Long floor;
        if ("M09.SHIFT".equals(type)) floor = store.shift(id, false).scopeId();
        else if ("M09.SCHEDULE".equals(type)) floor = store.schedule(id, false).scopeId();
        else throw new NotFoundException();
        return new Target(type, Long.toString(id), null, floor);
    }
}
