package com.eldercare.backend.admission.internal.registration.service;

import com.eldercare.backend.admission.api.events.ResidenceRegistrationReminder;
import com.eldercare.backend.admission.api.events.ResidenceRegistrationReminder.Kind;
import com.eldercare.backend.admission.internal.registration.domain.RegistrationObligation;
import com.eldercare.backend.admission.internal.registration.domain.ReminderType;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationObligationStore;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationReminderStore;
import com.eldercare.backend.admission.internal.transition.service.M02DailyTask;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc quá hạn khai báo (FR-002, FR-010; BR-M02-11; quickstart Q4): nghĩa vụ chưa ghi nộp mà hạn đã qua trước ngày {@code
 * day}, dù khai báo đã được lập hay chưa, thì nhắc Hành chính mỗi ngày (khóa {@code registration_reminder}) và báo Quản lý
 * viện một lần ({@code manager_notified_at}). Không lập khai báo (Q-306).
 */
@Component
@Order(30)
public class RegistrationOverdueReminders implements M02DailyTask {

    private final RegistrationObligationStore obligations;
    private final RegistrationReminderStore reminders;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    RegistrationOverdueReminders(
            RegistrationObligationStore obligations,
            RegistrationReminderStore reminders,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.obligations = obligations;
        this.reminders = reminders;
        this.events = events;
        this.time = time;
    }

    @Override
    public String name() {
        return "REG_OVERDUE";
    }

    /** Việc xét trạng thái tại ngày chạy nên ngày bị lỡ chỉ ghi bỏ qua; lần chạy đầu sau đó xử lý đủ (SC-002). */
    @Override
    public boolean latestDayOnly() {
        return true;
    }

    @Override
    public int run(LocalDate day) {
        return remind(day);
    }

    /** Nhắc của ngày {@code day}; trả số người được nhắc lần đầu trong ngày. */
    @Transactional
    public int remind(LocalDate day) {
        Instant dayStart = day.atStartOfDay(BusinessTime.ZONE).toInstant();
        Map<Long, List<RegistrationObligation>> byResident = obligations.overdue(dayStart).stream()
                .collect(Collectors.groupingBy(
                        RegistrationObligation::residentId, LinkedHashMap::new, Collectors.toList()));
        Instant now = time.now();
        int reminded = 0;
        for (Map.Entry<Long, List<RegistrationObligation>> resident : byResident.entrySet()) {
            long residentId = resident.getKey();
            if (reminders.claim(ReminderType.OVERDUE, residentId, day)) {
                events.publishEvent(new ResidenceRegistrationReminder(Kind.OVERDUE, residentId, day, now));
                reminded++;
            }
            boolean managerFirstTime = false;
            for (RegistrationObligation obligation : resident.getValue()) {
                if (obligation.managerNotifiedAt() == null && obligations.markManagerNotified(obligation.id(), now)) {
                    managerFirstTime = true;
                }
            }
            if (managerFirstTime) {
                events.publishEvent(new ResidenceRegistrationReminder(Kind.OVERDUE_MANAGER, residentId, day, now));
            }
        }
        return reminded;
    }
}
