package com.eldercare.backend.staffing.internal.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.staffing.credential.internal.persistence.CredentialStore;
import com.eldercare.backend.staffing.staff.internal.persistence.StaffStore;

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
