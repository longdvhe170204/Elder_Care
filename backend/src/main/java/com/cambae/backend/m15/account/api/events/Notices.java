package com.cambae.backend.m15.account.api.events;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceGroup;
import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.common.notification.Substitution;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Yêu cầu thông báo của sự kiện 002 (contracts/module-interfaces.md mục 1.10; 009 contracts mục 1.1). Mức để trống:
 * mục 17 không quy định cho các sự kiện này, 009 xếp theo nguyên tắc chung (009 FR-003, FR-043b); nội dung chỉ thông tin
 * chung.
 */
final class Notices {

    static final RecipientGroup MANAGERS = new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER);

    private Notices() {}

    /**
     * Người xem lại bản ghi ngoại tuyến (FR-041a, FR-041c, CHK056): Trưởng tầng của tầng, chưa có thì Người phụ trách ca
     * (cách thay mặc định của 009); không gắn tầng thì Quản lý viện.
     */
    static RecipientGroup reviewers(Long floorAreaId) {
        return floorAreaId == null
                ? MANAGERS
                : new RecipientGroup.FloorManager(floorAreaId, new Substitution.Default());
    }

    static NotificationRequest of(
            String eventType,
            SourceGroup group,
            SourceRecord source,
            String eventKey,
            Long floorAreaId,
            RecipientGroup recipients,
            String title,
            String text,
            Instant at) {
        return new NotificationRequest(
                "M15",
                eventType,
                group,
                source,
                eventKey,
                Optional.empty(),
                Optional.empty(),
                Optional.ofNullable(floorAreaId),
                List.of(recipients),
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(text),
                Optional.of(title),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
    }
}
