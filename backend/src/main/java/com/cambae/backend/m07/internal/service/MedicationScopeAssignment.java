package com.cambae.backend.m07.internal.service;

import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import org.springframework.stereotype.Component;

/**
 * Gắn phạm vi cho Danh mục thuốc (contracts mục 1.1; 002 FR-034).
 * Danh mục thuốc áp dụng toàn viện (không gắn người cao tuổi hay tầng cụ thể).
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
