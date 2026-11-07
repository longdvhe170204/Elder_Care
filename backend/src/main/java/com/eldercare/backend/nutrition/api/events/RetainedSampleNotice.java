package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.List;

/**
 * Thông báo của lưu mẫu thức ăn (FR-061, FR-062, FR-065), mức Nhẹ, không có tên người, dị ứng:
 *
 * <ul>
 *   <li>{@code NOT_RETAINED}: bản ghi có món không lưu mẫu (kèm lý do) → Bác sĩ, Quản lý viện;
 *   <li>{@code DISCARD_DUE}: mẫu quá lúc lưu + CFG-M08-06 mà chưa hủy → nhân viên bếp (một lần mỗi mẫu).
 * </ul>
 */
public record RetainedSampleNotice(long sampleId, long dailyMealId, Kind kind, int dishCount, Instant at)
        implements HasNotificationRequests {

    public enum Kind {
        NOT_RETAINED,
        DISCARD_DUE
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        return switch (kind) {
            case NOT_RETAINED ->
                List.of(PortionNotices.request(
                        "RETAINED_SAMPLE_NOT_RETAINED",
                        dailyMealId,
                        "M08.RETAINED_SAMPLE_NOT_RETAINED:" + sampleId + ":" + at.toEpochMilli(),
                        NotificationLevel.LOW,
                        null,
                        List.of(
                                new RecipientGroup.FacilityRole(Role.DOCTOR),
                                new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)),
                        dishCount + " món của bữa ăn không được lưu mẫu; xem lý do trong bản ghi lưu mẫu",
                        "Bữa ăn có món không lưu mẫu",
                        null,
                        at));
            case DISCARD_DUE ->
                List.of(PortionNotices.request(
                        "RETAINED_SAMPLE_DISCARD_DUE",
                        dailyMealId,
                        "M08.RETAINED_SAMPLE_DISCARD_DUE:" + sampleId,
                        NotificationLevel.LOW,
                        null,
                        List.of(new RecipientGroup.FacilityRole(Role.KITCHEN_STAFF)),
                        "Mẫu thức ăn đã quá thời hạn lưu; ghi thời điểm hủy mẫu",
                        "Mẫu thức ăn đến hạn hủy",
                        null,
                        at));
        };
    }
}
