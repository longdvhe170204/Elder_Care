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
        send(new M02Reminder(type, subjectId, day, at));
    }

    /** Chưa nhắc (loại, đối tượng) lần nào từ ngày {@code from} (demo 2026-11-25: báo người đại diện một lần). */
    public boolean notRemindedSince(String type, long subjectId, LocalDate from) {
        return store.lastReminded(type, subjectId).map(from::isAfter).orElse(true);
    }

    /** Như {@link #send(String, long, LocalDate, Instant)} với nội dung đọc được và báo người đại diện (M02Reminder). */
    @Transactional(propagation = Propagation.MANDATORY)
    public void send(M02Reminder reminder) {
        if (store.markReminded(reminder.type(), reminder.subjectId(), reminder.on())) {
            events.publishEvent(reminder);
        }
    }
}
