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
    private final String residentColumn;
    private final String condition;
    private final ResidentRecordStore records;

    public ChildScopeAssignment(String entityType, String table, ResidentRecordStore records) {
        this(entityType, table, "", records);
    }

    /** {@code condition}: điều kiện thêm cho bảng gộp nhiều loại (ví dụ {@code " AND kind = 'TREATMENT'"}). */
    public ChildScopeAssignment(String entityType, String table, String condition, ResidentRecordStore records) {
        this(entityType, table, "resident_id", condition, records);
    }

    /** {@code residentColumn}: cột mã hồ sơ khi không phải {@code resident_id} (bảng dùng chung {@code status_history}). */
    public ChildScopeAssignment(
            String entityType, String table, String residentColumn, String condition, ResidentRecordStore records) {
        this.entityType = entityType;
        this.table = table;
        this.residentColumn = residentColumn;
        this.condition = condition;
        this.records = records;
    }

    @Override
    public String entityType() {
        return entityType;
    }

    @Override
    public Target assignScope(long id) {
        Long residentId =
                records.residentIdOf(table, residentColumn, condition, id).orElse(null);
        return new Target(entityType, String.valueOf(id), residentId, null);
    }
}
