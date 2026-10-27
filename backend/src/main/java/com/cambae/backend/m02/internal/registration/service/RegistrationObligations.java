package com.cambae.backend.m02.internal.registration.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m02.api.events.ResidenceRegistrationReminder;
import com.cambae.backend.m02.internal.registration.domain.ObligationTrigger;
import com.cambae.backend.m02.internal.registration.domain.ReminderType;
import com.cambae.backend.m02.internal.registration.persistence.RegistrationObligationStore;
import com.cambae.backend.m02.internal.registration.persistence.RegistrationReminderStore;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi nghĩa vụ khai báo khi mốc phát sinh (FR-001, FR-001a, FR-002; research Z-03): hạn = mốc + CFG-M02-12 đọc lúc ghi,
 * nên đổi tham số sau không đổi hạn đã có (constitution XIII). Mốc mới thì nhắc Hành chính lập khai báo một lần (FR-010).
 * Không tạo khai báo (Q-306).
 */
@Service
class RegistrationObligations {

    private final RegistrationObligationStore store;
    private final RegistrationReminderStore reminders;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;

    RegistrationObligations(
            RegistrationObligationStore store,
            RegistrationReminderStore reminders,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            BusinessTime time) {
        this.store = store;
        this.reminders = reminders;
        this.parameters = parameters;
        this.events = events;
        this.time = time;
    }

    /** Ghi mốc {@code (người, loại mốc, nguồn)}; mốc đã có (sự kiện giao lại) thì không làm gì. */
    @Transactional(propagation = Propagation.MANDATORY)
    void record(long residentId, ObligationTrigger trigger, Instant triggerAt, long sourceId) {
        Instant dueAt = triggerAt.plus(parameters.read(ParameterCode.CFG_M02_12).asDuration());
        if (!store.record(residentId, trigger, triggerAt, dueAt, sourceId)) {
            return;
        }
        LocalDate today = time.today();
        if (reminders.claim(ReminderType.CREATE, residentId, today)) {
            events.publishEvent(new ResidenceRegistrationReminder(
                    ResidenceRegistrationReminder.Kind.TO_CREATE, residentId, today, time.now()));
        }
    }
}
