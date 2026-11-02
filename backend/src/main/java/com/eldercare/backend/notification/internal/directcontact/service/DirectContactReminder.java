package com.eldercare.backend.notification.internal.directcontact.service;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.internal.directcontact.persistence.DirectContactStore;
import com.eldercare.backend.notification.internal.job.service.M13DailyTask;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Đầu mỗi ngày còn mục Chờ liên hệ thì nhắc Hành chính, báo Quản lý viện (việc hằng ngày {@code
 * REMIND_DIRECT_CONTACTS}; FR-021). Gửi qua chính M13 như mọi module (FR-001); mức Nhẹ vì spec chưa nêu mức (mặc định
 * của Q-108). Mỗi ngày một yêu cầu (khóa theo ngày, FR-005).
 */
@Service
public class DirectContactReminder implements M13DailyTask {

    /** Sự kiện miền của M13: còn mục cần liên hệ trực tiếp. */
    public record DirectContactsPending(LocalDate day, int count, Instant at) implements HasNotificationRequests {

        @Override
        public List<NotificationRequest> notificationRequests() {
            String text = "Còn " + count + " người thân cần liên hệ trực tiếp";
            return List.of(new NotificationRequest(
                    "M13",
                    "DIRECT_CONTACT_PENDING",
                    SourceGroup.OTHER,
                    new SourceRecord("M13.DIRECT_CONTACT_REMINDER", day.toString()),
                    "DIRECT_CONTACT_PENDING:" + day,
                    Optional.of(NotificationLevel.LOW),
                    Optional.empty(),
                    Optional.empty(),
                    List.of(
                            new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                            new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)),
                    List.of(new ContentPart(InformationType.GENERAL, text)),
                    Optional.of(text),
                    Optional.of("Cần liên hệ trực tiếp"),
                    Optional.empty(),
                    false,
                    at,
                    Map.of("count", count),
                    false,
                    Optional.empty(),
                    Optional.empty()));
        }
    }

    private final DirectContactStore contacts;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate tx;
    private final BusinessTime time;

    DirectContactReminder(
            DirectContactStore contacts, ApplicationEventPublisher events, TransactionTemplate tx, BusinessTime time) {
        this.contacts = contacts;
        this.events = events;
        this.tx = tx;
        this.time = time;
    }

    @Override
    public String name() {
        return "REMIND_DIRECT_CONTACTS";
    }

    @Override
    public int run(LocalDate day) {
        Integer pending = tx.execute(s -> {
            Instant now = time.now();
            int count = contacts.markReminded(now);
            if (count > 0) {
                events.publishEvent(new DirectContactsPending(day, count, now));
            }
            return count;
        });
        return pending == null ? 0 : pending;
    }
}
