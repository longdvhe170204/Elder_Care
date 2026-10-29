package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import org.springframework.stereotype.Component;

/**
 * Scope assignment for Medication Catalog (contracts section 1.1; 002 FR-034).
 * Medication catalog applies facility-wide (not tied to specific resident or floor).
 */
@Component
public class MedicationScopeAssignment implements ScopeAssignmentProvider {

    public static final String TYPE = "M07.MEDICATION";

    @Override
    public String entityType() {
        return TYPE;
    }

    @Override
    public Target assignScope(long id) {
        return new Target(TYPE, Long.toString(id), null, null);
    }
}
