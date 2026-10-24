package com.cambae.backend.m01.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.m01.api.dto.CareLevel;
import java.time.Instant;
import java.util.List;

/**
 * Cần lập yêu cầu đổi mức, lần đầu; báo Hành chính mức Trung bình (H-14; FR-036, FR-054). Khóa sự kiện (residentId,
 * assessmentId). Mức mới chỉ ở nội dung đầy đủ; nội dung rút gọn không nêu.
 */
public record CareLevelChangeRequestNeeded(long residentId, long assessmentId, CareLevel newLevel, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "CARE_LEVEL_CHANGE_REQUEST_NEEDED",
                NotificationLevel.MEDIUM,
                new SourceRecord("M01.ASSESSMENT", String.valueOf(assessmentId)),
                "CARE_LEVEL_CHANGE_REQUEST_NEEDED:" + residentId + ":" + assessmentId,
                residentId,
                null,
                List.of(Notices.OFFICE),
                Notices.general("Đánh giá lại cho mức chăm sóc mới (" + newLevel + "): cần lập yêu cầu đổi mức."),
                "Cần lập yêu cầu đổi mức chăm sóc",
                "Có người cao tuổi cần lập yêu cầu đổi mức chăm sóc.",
                at));
    }
}
