package com.eldercare.backend.family.api.events;

import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Nhắc người cao tuổi có tài khoản về lượt thăm Đã duyệt trong ngày (FR-069, research F-18): mức Nhẹ, phần chung, nhóm
 * người cao tuổi của 009 (FR-022a); việc nửa đêm phát cho lượt của ngày, lượt lập trong ngày phát khi lập. Khóa
 * {@code (lượt thăm, RESIDENT_REMINDER)}; 009 giữ tới hết giờ yên tĩnh.
 */
public record ResidentVisitReminder(
        long visitId, long residentId, LocalTime startTime, LocalTime endTime, List<String> visitors, Instant at)
        implements HasNotificationRequests {

    private static final String TITLE = "Hôm nay có người đến thăm";

    public ResidentVisitReminder {
        visitors = List.copyOf(visitors);
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        String who = visitors.isEmpty() ? "" : ": " + String.join(", ", visitors);
        String summary = TITLE + " lúc " + startTime + "–" + endTime + who;
        return List.of(new NotificationRequest(
                "M10",
                "VISIT_RESIDENT_REMINDER",
                SourceGroup.FAMILY_INFO,
                new SourceRecord(VisitCancelledNotice.SOURCE_TYPE, String.valueOf(visitId)),
                VisitCancelledNotice.SOURCE_TYPE + ":" + visitId + ":RESIDENT_REMINDER",
                Optional.of(NotificationLevel.LOW),
                Optional.of(residentId),
                Optional.empty(),
                List.of(new RecipientGroup.Resident(residentId)),
                List.of(new ContentPart(InformationType.GENERAL, summary)),
                Optional.of(summary),
                Optional.of(TITLE),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty()));
    }
}
