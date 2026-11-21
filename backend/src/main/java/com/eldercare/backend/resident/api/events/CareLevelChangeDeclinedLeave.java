package com.eldercare.backend.resident.api.events;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.util.List;

/**
 * Gia đình không đồng ý đổi mức chăm sóc và chọn rời viện (001 FR-036b, Q-423): báo Hành chính lập hồ sơ kết thúc lưu trú
 * (004); hệ thống không tự lập (Q-306). Mức Nhẹ.
 */
public record CareLevelChangeDeclinedLeave(long residentId, long decisionId, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "CARE_LEVEL_DECLINED_LEAVE",
                NotificationLevel.LOW,
                new SourceRecord("M01.RESIDENT", String.valueOf(residentId)),
                "CARE_LEVEL_DECLINED_LEAVE:" + decisionId,
                residentId,
                null,
                List.of(Notices.OFFICE),
                Notices.general("Gia đình không đồng ý đổi mức chăm sóc và chọn rời viện. Lập hồ sơ kết thúc lưu trú."),
                "Gia đình chọn rời viện",
                "Gia đình chọn rời viện thay vì đổi mức chăm sóc; lập hồ sơ kết thúc lưu trú.",
                at));
    }
}
