package com.eldercare.backend.nutrition.api.events;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import java.time.Instant;
import java.util.List;

/**
 * Phiếu hoặc phần bổ sung Có sai lệch, hoặc sai lệch đã được xử lý (bảng phiếu, FR-048b; FR-065): báo → nhân viên bếp mức
 * Trung bình; xử lý → người đã báo sai lệch mức Nhẹ. Nội dung không có tên người, dị ứng (SC-008).
 */
public record SlipDiscrepancyNotice(
        long slipId,
        long dailyMealId,
        boolean resolved,
        boolean onSupplement,
        Long reporterStaffId,
        String eventKey,
        Long causedBy,
        Instant at)
        implements HasNotificationRequests {

    @Override
    public List<NotificationRequest> notificationRequests() {
        String what = onSupplement ? "Phần bổ sung của phiếu" : "Phiếu";
        if (!resolved) {
            return List.of(PortionNotices.request(
                    "MEAL_SLIP_DISCREPANCY_REPORTED",
                    dailyMealId,
                    eventKey,
                    NotificationLevel.MEDIUM,
                    null,
                    List.of(new RecipientGroup.FacilityRole(Role.KITCHEN_STAFF)),
                    what + " bữa ăn được báo sai lệch; cần ghi cách xử lý",
                    "Phiếu có sai lệch",
                    causedBy,
                    at));
        }
        if (reporterStaffId == null) {
            return List.of();
        }
        return List.of(PortionNotices.request(
                "MEAL_SLIP_DISCREPANCY_RESOLVED",
                dailyMealId,
                eventKey,
                NotificationLevel.LOW,
                null,
                List.of(new RecipientGroup.Staff(reporterStaffId, "DISCREPANCY_REPORTER")),
                "Bếp đã ghi cách xử lý sai lệch " + (onSupplement ? "phần bổ sung" : "phiếu") + " bạn đã báo",
                "Sai lệch đã xử lý",
                causedBy,
                at));
    }
}
