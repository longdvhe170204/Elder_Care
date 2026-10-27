package com.cambae.backend.m13.internal.urgent.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.ContentPart;
import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.InformationSubject;
import com.cambae.backend.common.notification.InformationType;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceGroup;
import com.cambae.backend.common.notification.SourceRecord;
import com.cambae.backend.m13.internal.urgent.persistence.CallReminderStore.Reminder;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.dto.AccountView;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Thông báo của lượt nhắc gọi, gửi qua chính M13 như mọi module (FR-001; research B-08: nguồn {@code M13.CALL_REMINDER},
 * khóa theo lượt và lần chuyển). Lượt nhắc là thông báo Khẩn cấp (FR-027); số điện thoại không đặt trong thông báo mà hiện
 * lúc mở lượt. Tin báo người được nhắc (đã chuyển, đã dừng, nội dung cập nhật, người khác đã ghi kết quả) và báo các Quản lý
 * viện khác: spec không nêu mức nên dùng Nhẹ theo mặc định Q-108. Báo nhiều Quản lý viện dùng nhóm vai trò toàn viện (một
 * yêu cầu, sự kiện ngắn vì sự kiện lưu ở outbox giới hạn độ dài). Thông báo do M13 gửi không sinh lượt nhắc gọi.
 */
@Component
class CallReminderNotices {

    static final String SOURCE = "M13.CALL_REMINDER";

    /** Độ dài tối đa của nội dung cần báo trong thông báo nhắc (thông số kỹ thuật). */
    static final int CONTENT_LIMIT = 500;

    /** Sự kiện miền của M13 mang yêu cầu thông báo của lượt nhắc gọi. */
    record CallReminderNotice(List<NotificationRequest> notificationRequests) implements HasNotificationRequests {}

    private final ApplicationEventPublisher events;
    private final AccountApi accounts;

    CallReminderNotices(ApplicationEventPublisher events, AccountApi accounts) {
        this.events = events;
        this.accounts = accounts;
    }

    /** Nhắc gọi (Khẩn cấp) tới người được nhắc của lần chuyển {@code handoffId}. */
    void remind(Reminder r, long staffId, long handoffId, String contentToReport, Instant at) {
        // Nội dung gộp đầy đủ xem ở chi tiết lượt; thông báo giữ ngắn để vừa outbox
        String content = contentToReport.length() > CONTENT_LIMIT
                ? contentToReport.substring(0, CONTENT_LIMIT) + "…"
                : contentToReport;
        String text = "Cần gọi điện báo tin khẩn cấp: " + content
                + ". Mở lượt nhắc gọi để xem người cần gọi và số điện thoại.";
        publish(
                r,
                "CALL_REMINDER",
                "CALL_REMINDER:" + r.id() + ":" + handoffId,
                NotificationLevel.URGENT,
                List.of(staff(staffId)),
                Optional.empty(),
                "Nhắc gọi điện",
                text,
                "Có lượt nhắc gọi điện khẩn cấp, vui lòng mở ứng dụng",
                at);
    }

    /** Tin báo mức Nhẹ cho từng người được nhắc. */
    void inform(Reminder r, List<Long> staffIds, String eventType, String key, String text, Instant at) {
        for (long staffId : staffIds) {
            publish(
                    r,
                    eventType,
                    eventType + ":" + r.id() + ":" + key + ":" + staffId,
                    NotificationLevel.LOW,
                    List.of(staff(staffId)),
                    Optional.empty(),
                    "Lượt nhắc gọi",
                    text,
                    text,
                    at);
        }
    }

    /** Báo các Quản lý viện khác (mức Nhẹ) khi lượt ở bậc Quản lý viện; người đang giữ lượt không nhận (FR-012). */
    void informOtherManagers(Reminder r, long assignedStaffId, String key, Instant at) {
        String text = "Có lượt nhắc gọi khẩn cấp đang chờ Quản lý viện gọi";
        publish(
                r,
                "CALL_REMINDER_AT_MANAGER",
                "CALL_REMINDER_AT_MANAGER:" + r.id() + ":" + key,
                NotificationLevel.LOW,
                List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)),
                accounts.byStaff(assignedStaffId).map(AccountView::accountId),
                "Lượt nhắc gọi",
                text,
                text,
                at);
    }

    /**
     * Hết người để gọi trong chuỗi: báo Quản lý viện bằng thông báo Khẩn cấp kèm các cuộc gọi đã thực hiện (FR-029a). Người
     * cần gọi là Quản lý viện thì chỉ báo các Quản lý viện khác.
     */
    void chainNotReached(Reminder r, Optional<List<Long>> otherManagersOnly, String calls, Instant at) {
        List<RecipientGroup> groups = otherManagersOnly
                .map(ids -> ids.stream().map(CallReminderNotices::staff).toList())
                .orElse(List.of(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)));
        if (groups.isEmpty()) {
            return;
        }
        publish(
                r,
                "CALL_CHAIN_NOT_REACHED",
                "CALL_CHAIN_NOT_REACHED:" + r.chainId(),
                NotificationLevel.URGENT,
                groups,
                Optional.empty(),
                "Không liên lạc được",
                "Không liên lạc được người cần báo tin khẩn cấp. Các cuộc gọi đã thực hiện: " + calls,
                "Không liên lạc được người cần báo tin khẩn cấp, vui lòng mở ứng dụng",
                at);
    }

    private static RecipientGroup staff(long staffId) {
        return new RecipientGroup.Staff(staffId, "CALL_REMINDER");
    }

    private void publish(
            Reminder r,
            String eventType,
            String key,
            NotificationLevel level,
            List<RecipientGroup> groups,
            Optional<Long> causedByAccount,
            String title,
            String text,
            String shortContent,
            Instant at) {
        events.publishEvent(new CallReminderNotice(List.of(new NotificationRequest(
                "M13",
                eventType,
                SourceGroup.OTHER,
                new SourceRecord(SOURCE, Long.toString(r.id())),
                key,
                Optional.of(level),
                Optional.ofNullable(r.residentId()),
                Optional.ofNullable(r.floorAreaId()),
                groups,
                List.of(new ContentPart(InformationType.GENERAL, text)),
                Optional.of(shortContent),
                Optional.of(title),
                causedByAccount,
                false,
                at,
                Map.of(),
                false,
                r.residentId() == null ? Optional.empty() : Optional.of(InformationSubject.ABOUT_RESIDENT),
                Optional.empty()))));
    }
}
