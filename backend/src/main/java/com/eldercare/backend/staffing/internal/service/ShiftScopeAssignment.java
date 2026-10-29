package com.eldercare.backend.staffing.internal.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;

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
