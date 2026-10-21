package com.cambae.backend.m13.internal.governance.service;

import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.ScopeAssignmentProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Gắn phạm vi cho thông báo ({@code GanPhamViProvider} {@code M13.THONG_BAO}; contracts mục 1.3, 002 FR-034): người cao
 * tuổi và tầng/khu vực của yêu cầu, để Trưởng tầng tra cứu thông báo của tầng được giao (FR-037).
 */
@Component
class NotificationScopeAssignment implements ScopeAssignmentProvider {

    static final String TYPE = "M13.NOTIFICATION";

    private final JdbcTemplate jdbc;

    NotificationScopeAssignment(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public String entityType() {
        return TYPE;
    }

    @Override
    public Target assignScope(long id) {
        return jdbc
                .query(
                        "SELECT r.resident_id, r.floor_area_id FROM notification n JOIN notification_request r"
                                + " ON r.id = n.request_id WHERE n.id = ?",
                        (rs, i) -> new Target(
                                TYPE, Long.toString(id), rs.getObject(1, Long.class), rs.getObject(2, Long.class)),
                        id)
                .stream()
                .findFirst()
                .orElse(new Target(TYPE, Long.toString(id), null, null));
    }
}
