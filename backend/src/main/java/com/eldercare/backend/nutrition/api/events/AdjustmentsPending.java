package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Phát sinh cần bếp xác nhận (FR-039 → FR-041, BR-M08-13; FR-065): phát sinh mới sau chốt, yêu cầu suất bổ sung → bếp mức
 * Trung bình; chưa xác nhận tại CFG-M08-05 trước giờ bữa, hoặc tạo sau mốc đó ({@code reminder}) → bếp và Bác sĩ mức Trung
 * bình. {@code eventKey} là khóa của lần báo (phát sinh hoặc mốc nhắc).
 */
public record AdjustmentsPending(
        long dailyMealId, LocalDate date, int count, boolean reminder, String eventKey, Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        List<RecipientGroup> recipients = new ArrayList<>(List.of(new RecipientGroup.FacilityRole(Role.KITCHEN_STAFF)));
        if (reminder) {
            recipients.add(new RecipientGroup.FacilityRole(Role.DOCTOR));
        }
        return List.of(PortionNotices.request(
                reminder ? "ADJUSTMENT_CONFIRMATION_DUE" : "ADJUSTMENT_CREATED",
                dailyMealId,
                eventKey,
                NotificationLevel.MEDIUM,
                null,
                recipients,
                (reminder ? "Còn " + count + " phát sinh chưa được bếp xác nhận" : count + " phát sinh mới")
                        + " ở bữa ngày " + date,
                reminder ? "Phát sinh chưa xác nhận" : "Phát sinh sau chốt",
                null,
                at));
    }
}
