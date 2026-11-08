package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tại CFG-M08-07 tuần kế tiếp chưa có thực đơn Công bố (FR-027, BR-M08-16; FR-065): nhắc nhân viên bếp mức Trung bình,
 * Bác sĩ mức Trung bình khi thực đơn đang Chờ công bố, Quản lý viện mức Nhẹ. Bản ghi nguồn là tuần (chưa chắc có thực đơn).
 */
public record MenuPublicationReminder(LocalDate weekStart, boolean awaitingPublication, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<NotificationRequest> out = new ArrayList<>();
        out.add(request(Role.KITCHEN_STAFF, NotificationLevel.MEDIUM));
        if (awaitingPublication) {
            out.add(request(Role.DOCTOR, NotificationLevel.MEDIUM));
        }
        out.add(request(Role.NURSING_HOME_MANAGER, NotificationLevel.LOW));
        return out;
    }

    private NotificationRequest request(Role role, NotificationLevel level) {
        String text = awaitingPublication
                ? "Thực đơn tuần " + weekStart + " đang chờ Bác sĩ công bố"
                : "Tuần " + weekStart + " chưa có thực đơn Công bố";
        return new NotificationRequest(
                "M08",
                "MENU_PUBLICATION_REMINDER",
                SourceGroup.OTHER,
                new SourceRecord("M08.MENU_WEEK", weekStart.toString()),
                "M08.MENU_PUBLICATION_REMINDER:" + weekStart + ":" + role.name(),
                Optional.of(level),
                Optional.empty(),
                Optional.empty(),
                List.of(new RecipientGroup.FacilityRole(role)),
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.empty(),
                Optional.of("Nhắc công bố thực đơn tuần"),
                Optional.empty(),
                false,
                at,
                Map.of("weekStart", weekStart.toString()),
                false,
                Optional.empty(),
                Optional.empty());
    }
}
