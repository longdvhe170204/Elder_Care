package com.cambae.backend.m02.internal.transition.service;

import com.cambae.backend.m02.api.events.M02Reminder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gửi một lần nhắc của M02 và ghi vào {@code m02_reminder} (loại nhắc, đối tượng, lần nhắc cuối, số lần; data-model
 * mục 11). Mỗi ngày mỗi (loại, đối tượng) nhắc một lần vì mỗi việc hằng ngày chạy đúng một lần mỗi ngày.
 */
@Component
public class ReminderLog {

    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;

    ReminderLog(JdbcTemplate jdbc, ApplicationEventPublisher events) {
        this.jdbc = jdbc;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void send(String type, long subjectId, LocalDate day, Instant at) {
        jdbc.update(
                "INSERT INTO m02_reminder (type, subject_id, last_reminded_at, `count`) VALUES (?, ?, ?, 1) ON"
                        + " DUPLICATE KEY UPDATE last_reminded_at = VALUES(last_reminded_at), `count` = `count` + 1",
                type,
                subjectId,
                LocalDateTime.ofInstant(at, ZoneOffset.UTC));
        events.publishEvent(new M02Reminder(type, subjectId, day, at));
    }
}
