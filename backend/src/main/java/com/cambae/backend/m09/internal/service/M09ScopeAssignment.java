package com.cambae.backend.m09.internal.service;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m09.credential.internal.persistence.CredentialStore;
import com.cambae.backend.m09.staff.internal.persistence.StaffStore;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;

/** T009: hồ sơ/chứng chỉ gắn nhân viên, không gán nhầm nhân viên thành người cao tuổi hoặc tầng. */
public class M09ScopeAssignment implements ScopeAssignmentProvider {
    private final String type;
    private final StaffStore staff;
    private final CredentialStore credentials;

    public M09ScopeAssignment(String type, StaffStore staff, CredentialStore credentials) {
        this.type = type;
        this.staff = staff;
        this.credentials = credentials;
    }

    public String entityType() {
        return type;
    }

    public Target assignScope(long id) {
        if ("M09.STAFF".equals(type)) staff.find(id).orElseThrow(NotFoundException::new);
        else credentials.find(id).orElseThrow(NotFoundException::new);
        return new Target(type, Long.toString(id), null, null);
    }
}
