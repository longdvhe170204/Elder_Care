package com.cambae.backend.m01.internal.profile.service;

import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import org.springframework.stereotype.Component;

/**
 * Gắn phạm vi cho hồ sơ người cao tuổi (001 T011; 002 contracts mục 1.9): {@code Target} mang {@code residentId} để 002
 * tính phạm vi theo phân bổ giường và quan hệ người thân. Tầng/khu vực không nằm ở M01 mà lấy từ phân bổ giường của
 * 003, nên để trống.
 */
@Component
class ResidentScopeAssignment implements ScopeAssignmentProvider {

    static final String ENTITY_TYPE = "M01.RESIDENT";

    @Override
    public String entityType() {
        return ENTITY_TYPE;
    }

    @Override
    public Target assignScope(long id) {
        return new Target(ENTITY_TYPE, String.valueOf(id), id, null);
    }
}
