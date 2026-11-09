package com.eldercare.backend.care.internal.session.service;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.dto.AccountStatus;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.ActivityResidentReminder;
import com.eldercare.backend.care.internal.activity.service.ActivityScheduling;
import com.eldercare.backend.care.internal.scheduling.service.CareDailyWork;
import com.eldercare.backend.care.internal.session.domain.Registration;
import com.eldercare.backend.care.internal.session.domain.Session;
import com.eldercare.backend.care.internal.session.persistence.ActivitySessionStore;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T033 (FR-071 dòng bổ sung Q-366; research L-16): nhắc chính người cao tuổi có tài khoản Hoạt động về buổi trong
 * ngày đã có tên, ở việc hằng ngày của {@code CARE_DAILY} và khi đăng ký trong ngày; khóa {@code (đăng ký, NHAC_NCT)} ở
 * {@code reminder_marker}. Chỉ nhắc, không tạo bản ghi (Q-306). Buổi chuyến đi không thuộc lần này.
 */
@Component
public class ResidentSessionReminders implements CareDailyWork {
    static final String KIND = "M04.RESIDENT_SESSION_REMINDER";

    private final ActivitySessionStore store;
    private final AccountApi accounts;
    private final ReminderMarkers markers;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    public ResidentSessionReminders(
            ActivitySessionStore store,
            AccountApi accounts,
            ReminderMarkers markers,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.store = store;
        this.accounts = accounts;
        this.markers = markers;
        this.events = events;
        this.time = time;
    }

    @Override
    public String name() {
        return ActivityScheduling.PREFIX + "RESIDENT_SESSION_REMINDER";
    }

    /** Chỉ ngày hôm nay: nhắc ngày đã qua không còn ý nghĩa. */
    @Override
    public List<LocalDate> dueDates(LocalDate today) {
        return List.of(today);
    }

    @Override
    @Transactional
    public int run(LocalDate date, Instant now) {
        int count = 0;
        for (var session : store.scheduledOn(date))
            for (var r : store.activeRegistrations(session.id()))
                if (Registration.REGISTERED.equals(r.status()) && remind(session, r.id(), r.residentId(), now)) count++;
        return count;
    }

    /** Đăng ký trong ngày của buổi chưa bắt đầu (gọi trong transaction của lệnh đăng ký). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registered(Session session, long registrationId, long residentId) {
        var now = time.now();
        if (session.tripStatus() != null || !session.startsAt().isAfter(now)) return;
        if (!LocalDate.ofInstant(session.startsAt(), BusinessTime.ZONE).equals(time.today())) return;
        remind(session, registrationId, residentId, now);
    }

    private boolean remind(Session session, long registrationId, long residentId, Instant now) {
        boolean active = accounts.byResident(residentId)
                .filter(a -> a.status() == AccountStatus.ACTIVE)
                .isPresent();
        if (!active) return false;
        if (!markers.mark(KIND, "M04.ACTIVITY_REGISTRATION", registrationId, "NHAC_NCT", null)) return false;
        events.publishEvent(new ActivityResidentReminder(
                session.id(),
                registrationId,
                residentId,
                new CareEventContext(
                        "M04.ACTIVITY_REGISTRATION",
                        registrationId,
                        "NHAC_NCT:" + registrationId,
                        now,
                        Optional.ofNullable(session.floorAreaId()),
                        List.of(new RecipientGroup.Resident(residentId)))));
        return true;
    }
}
