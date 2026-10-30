package com.eldercare.backend.resident.internal.profile.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.resident.internal.persistence.ResidentRecordStore;

/**
 * Gắn phạm vi cho một loại bản ghi con của hồ sơ người cao tuổi (001 T011; 002 contracts mục 1.9; FR-034): {@code Target}
 * mang {@code residentId} của chủ bản ghi để 002 tính phạm vi theo phân bổ giường và quan hệ người thân. Tầng/khu vực
 * để trống (lấy từ phân bổ giường của 003). Mã không tồn tại thì không có người cao tuổi, nên không mở phạm vi nào.
 */
public class ChildScopeAssignment implements ScopeAssignmentProvider {

    private final String entityType;
    private final String table;
    private final ResidentRecordStore records;

    public ChildScopeAssignment(String entityType, String table, ResidentRecordStore records) {
        this.entityType = entityType;
        this.table = table;
        this.records = records;
    }

    @Override
    public String entityType() {
        return entityType;
    }

    @Override
    public Target assignScope(long id) {
        Long residentId = records.residentIdOf(table, id).orElse(null);
        return new Target(entityType, String.valueOf(id), residentId, null);
    }
}
