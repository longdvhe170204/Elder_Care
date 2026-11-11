package com.eldercare.backend.staffing.internal.swap.service;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.api.events.SwapNotification;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thông báo FR-044: mức Nhẹ cho đổi ca mới, đồng ý hoặc từ chối, kết quả duyệt, hết hiệu lực; mức Trung bình cho nhắc sắp
 * tới giờ ca và yêu cầu nghỉ mới. Nội dung chỉ nêu sự kiện, mã yêu cầu và ca, không nêu lý do (FR-045).
 */
@Component
class SwapNotices {

    private static final Set<String> MEDIUM = Set.of("LEAVE_REQUESTED", "REQUEST_DUE_SOON");

    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    SwapNotices(ApplicationEventPublisher events, BusinessTime time) {
        this.events = events;
        this.time = time;
    }

    /**
     * @param sourceType {@code M09.SHIFT_SWAP} hoặc {@code M09.LEAVE_REQUEST}
     * @param staffIds nhân viên nhận; {@code floorAreaId} khác {@code null} thì báo cả Trưởng tầng; {@code toManager} thêm
     *     Quản lý viện
     */
    void send(
            String sourceType,
            long requestId,
            String event,
            String text,
            List<Long> staffIds,
            Long floorAreaId,
            boolean toFloorManager,
            boolean toManager) {
        Instant at = time.now();
        List<RecipientGroup> groups = new ArrayList<>();
        staffIds.stream().distinct().sorted().forEach(id -> groups.add(new RecipientGroup.Staff(id, event)));
        if (toFloorManager && floorAreaId != null) {
            groups.add(new RecipientGroup.FloorManager(floorAreaId, new Substitution.Default()));
        }
        if (toManager || toFloorManager && floorAreaId == null) {
            groups.add(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        }
        if (groups.isEmpty()) {
            return;
        }
        String message = "Yêu cầu #" + requestId + ": " + text + ". Mở ứng dụng để xem.";
        events.publishEvent(new SwapNotification(List.of(new NotificationRequest(
                "M09",
                event,
                SourceGroup.SCHEDULE_CHANGE,
                new SourceRecord(sourceType, Long.toString(requestId)),
                event + ":" + requestId + ":" + at,
                Optional.of(MEDIUM.contains(event) ? NotificationLevel.MEDIUM : NotificationLevel.LOW),
                Optional.empty(),
                Optional.ofNullable(floorAreaId),
                groups,
                List.of(new ContentPart(InformationType.GENERAL, message)),
                Optional.of(message),
                Optional.of(text),
                Optional.empty(),
                false,
                at,
                Map.of("requestId", requestId),
                false,
                Optional.empty(),
                Optional.empty()))));
    }
}
