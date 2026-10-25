package com.cambae.backend.m02.internal.application.service;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.m02.internal.application.domain.AdmissionApplication;
import com.cambae.backend.m02.internal.application.persistence.AdmissionApplicationStore;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gắn phạm vi cho lượt đăng ký tiếp nhận (004 T011; 002 contracts mục 1.9): {@code Target} mang người cao tuổi của
 * lượt để 002 tính phạm vi theo phân bổ giường và quan hệ người thân; tầng lấy từ phân bổ giường của 003 nên để trống.
 */
@Component
class AdmissionApplicationScopeAssignment implements ScopeAssignmentProvider {

    private final AdmissionApplicationStore store;

    AdmissionApplicationScopeAssignment(AdmissionApplicationStore store) {
        this.store = store;
    }

    @Override
    public String entityType() {
        return AdmissionApplicationService.ENTITY_TYPE;
    }

    @Override
    @Transactional(readOnly = true)
    public Target assignScope(long id) {
        AdmissionApplication application = store.find(id).orElseThrow(NotFoundException::new);
        return AdmissionApplicationService.target(application.id(), application.residentId());
    }
}
