package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.events.ResidenceRegistrationReminder;
import com.cambae.backend.m02.api.events.ResidenceRegistrationReminder.Kind;
import com.cambae.backend.m02.internal.registration.domain.ReminderType;
import com.cambae.backend.m02.internal.registration.domain.ResidenceRegistration;
import com.cambae.backend.m02.internal.registration.persistence.RegistrationReminderStore;
import com.cambae.backend.m02.internal.registration.persistence.ResidenceRegistrationStore;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nhắc lập khai báo gia hạn (FR-006, BR-M02-12; quickstart Q11): Đăng ký tạm trú Đã xác nhận còn CFG-M02-13 tới ngày hết
 * hạn hiện hành (gồm cả ngày hết hạn), người cao tuổi chưa ở trạng thái cuối, chưa có khai báo đang xử lý thì nhắc Hành
 * chính mỗi ngày (khóa {@code registration_reminder} loại {@code RENEWAL}). Không lập khai báo (Q-306).
 *
 * <p>Chưa là {@link RegistrationDailyTask} vì job khai báo chưa có cấu hình profile test (BL-90); khi A xong, cài interface
 * với tên việc {@code REG_RENEWAL} như {@link RegistrationOverdueReminders}.
 */
@Service
public class RegistrationRenewalReminders {

    static final Set<ResidentStatus> FINAL =
            Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);

    private final ResidenceRegistrationStore registrations;
    private final RegistrationExpiryDates expiryDates;
    private final RegistrationReminderStore reminders;
    private final ResidentApi residents;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    RegistrationRenewalReminders(
            ResidenceRegistrationStore registrations,
            RegistrationExpiryDates expiryDates,
            RegistrationReminderStore reminders,
            ResidentApi residents,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.registrations = registrations;
        this.expiryDates = expiryDates;
        this.reminders = reminders;
        this.residents = residents;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    /** Nhắc của ngày {@code day}; trả số người được nhắc lần đầu trong ngày. */
    @Transactional
    public int remind(LocalDate day) {
        int reminded = 0;
        for (ResidenceRegistration registration : due(day)) {
            if (reminders.claim(ReminderType.RENEWAL, registration.residentId(), day)) {
                events.publishEvent(
                        new ResidenceRegistrationReminder(Kind.RENEWAL, registration.residentId(), day, time.now()));
                reminded++;
            }
        }
        return reminded;
    }

    /** Đăng ký tạm trú cần lập khai báo gia hạn vào ngày {@code day} (cũng là việc "sắp hết hạn" của danh sách cần làm). */
    List<ResidenceRegistration> due(LocalDate day) {
        long window = parameters.read(ParameterCode.CFG_M02_13).asDuration().toDays();
        List<ResidenceRegistration> confirmed = registrations.confirmedTemporaryResidence();
        Map<Long, LocalDate> expiry = expiryDates.of(confirmed);
        Set<Long> inProgress = registrations.residentsWithInProgress();
        return confirmed.stream()
                .filter(r -> expiry.containsKey(r.id()))
                .filter(r -> !day.isBefore(expiry.get(r.id()).minusDays(window)) && !day.isAfter(expiry.get(r.id())))
                .filter(r -> !inProgress.contains(r.residentId()))
                .filter(r -> !FINAL.contains(residents.status(r.residentId())))
                .toList();
    }

    /** Ngày hết hạn hiện hành của các khai báo (theo đính chính). */
    Map<Long, LocalDate> expiryOf(List<ResidenceRegistration> list) {
        return expiryDates.of(list);
    }
}
