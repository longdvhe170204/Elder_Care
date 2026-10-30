package com.eldercare.backend.notification.internal.governance.service;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.notification.internal.persistence.NotificationStore;
import org.springframework.stereotype.Component;

/**
 * Gắn phạm vi cho thông báo ({@code GanPhamViProvider} {@code M13.THONG_BAO}; contracts mục 1.3, 002 FR-034): người cao
 * tuổi và tầng/khu vực của yêu cầu, để Trưởng tầng tra cứu thông báo của tầng được giao (FR-037).
 */
@Component
class NotificationScopeAssignment implements ScopeAssignmentProvider {

    static final String TYPE = "M13.NOTIFICATION";

    private final NotificationStore notifications;

    NotificationScopeAssignment(NotificationStore notifications) {
        this.notifications = notifications;
    }

    @Override
    public String entityType() {
        return TYPE;
    }

    @Override
    public Target assignScope(long id) {
        return notifications
                .scope(id)
                .map(s -> new Target(TYPE, Long.toString(id), s.residentId(), s.floorAreaId()))
                .orElse(new Target(TYPE, Long.toString(id), null, null));
    }
}
