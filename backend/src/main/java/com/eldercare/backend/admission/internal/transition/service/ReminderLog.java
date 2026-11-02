package com.eldercare.backend.admission.internal.transition.service;

import com.eldercare.backend.admission.api.events.M02Reminder;
import com.eldercare.backend.admission.internal.persistence.M02RunStore;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gửi một lần nhắc của M02 và ghi dấu vào {@code reminder_marker} (data-model mục 0, 11): mỗi ngày mỗi (loại, đối tượng) nhắc tối
 * đa một lần.
 */
@Component
public class ReminderLog {

    private final M02RunStore store;
    private final ApplicationEventPublisher events;

    ReminderLog(M02RunStore store, ApplicationEventPublisher events) {
        this.store = store;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void send(String type, long subjectId, LocalDate day, Instant at) {
        if (store.markReminded(type, subjectId, day)) {
            events.publishEvent(new M02Reminder(type, subjectId, day, at));
        }
    }
}
