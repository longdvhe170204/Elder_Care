package com.cambae.backend.m01.internal.profile.service;

import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Gắn phạm vi cho một loại bản ghi con của hồ sơ người cao tuổi (001 T011; 002 contracts mục 1.9; FR-034): {@code Target}
 * mang {@code residentId} của chủ bản ghi để 002 tính phạm vi theo phân bổ giường và quan hệ người thân. Tầng/khu vực
 * để trống (lấy từ phân bổ giường của 003). Mã không tồn tại thì không có người cao tuổi, nên không mở phạm vi nào.
 */
class ChildScopeAssignment implements ScopeAssignmentProvider {

    private final String entityType;
    private final String table;
    private final JdbcTemplate jdbc;

    ChildScopeAssignment(String entityType, String table, JdbcTemplate jdbc) {
        this.entityType = entityType;
        this.table = table;
        this.jdbc = jdbc;
    }

    @Override
    public String entityType() {
        return entityType;
    }

    @Override
    public Target assignScope(long id) {
        Long residentId =
                jdbc.queryForList("SELECT resident_id FROM " + table + " WHERE id = ?", Long.class, id).stream()
                        .findFirst()
                        .orElse(null);
        return new Target(entityType, String.valueOf(id), residentId, null);
    }
}
