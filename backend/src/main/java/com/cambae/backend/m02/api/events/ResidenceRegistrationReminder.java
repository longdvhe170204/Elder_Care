package com.cambae.backend.m02.api.events;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationLevel;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.notification.RecipientGroup;
import com.cambae.backend.common.notification.SourceRecord;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Nhắc khai báo tạm trú, lưu trú của 018, mức Nhẹ (FR-010): nhắc lập khi nghĩa vụ phát sinh, quá hạn (Hành chính mỗi ngày,
 * Quản lý viện một lần), nhắc lập khai báo gia hạn (Hành chính mỗi ngày, FR-006), báo Quản lý viện khi khai báo Hết hạn, nhắc khai báo xóa (Hành chính mỗi CFG-M02-09, FR-008). {@code on} là ngày nhắc nên mỗi (loại, người, ngày) một thông báo; nội dung chỉ thông tin chung.
 */
public record ResidenceRegistrationReminder(Kind kind, long residentId, LocalDate on, Instant at)
        implements HasNotificationRequests {

    public enum Kind {
        TO_CREATE(Notices.OFFICE, "Cần lập khai báo tạm trú, lưu trú cho người cao tuổi nội trú."),
        OVERDUE(Notices.OFFICE, "Khai báo tạm trú, lưu trú đã quá hạn mà chưa ghi nộp."),
        OVERDUE_MANAGER(Notices.MANAGER, "Có khai báo tạm trú, lưu trú quá hạn mà chưa ghi nộp."),
        RENEWAL(Notices.OFFICE, "Đăng ký tạm trú sắp hết hạn, cần lập khai báo gia hạn."),
        EXPIRED_MANAGER(Notices.MANAGER, "Có đăng ký tạm trú đã hết hạn mà chưa có khai báo gia hạn được xác nhận."),
        DEREGISTER(Notices.OFFICE, "Người cao tuổi đã kết thúc lưu trú hoặc qua đời, cần khai báo xóa tạm trú.");

        private final RecipientGroup recipients;
        private final String text;

        Kind(RecipientGroup recipients, String text) {
            this.recipients = recipients;
            this.text = text;
        }
    }

    @Override
    public List<NotificationRequest> notificationRequests() {
        return List.of(Notices.of(
                "M02_REGISTRATION_" + kind,
                new SourceRecord("M02.REGISTRATION_REMINDER", String.valueOf(residentId)),
                "M02_REGISTRATION:" + kind + ":" + residentId + ":" + on,
                NotificationLevel.LOW,
                residentId,
                kind.recipients,
                "Khai báo tạm trú, lưu trú",
                kind.text,
                at));
    }
}
