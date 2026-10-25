package com.cambae.backend.m02.internal.transition.service;

import java.time.LocalDate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Việc {@code REMINDERS} của job hằng ngày (research T-11; FR-077): chạy mọi {@link ReminderRule} của các user story.
 * Chỉ có nghĩa với ngày mới nhất nên nhắc bị lỡ gộp thành một lần (BR-M02-19). Nhắc không tạo bản ghi nghiệp vụ nào
 * (000 FR-010a).
 */
@Component
@Order(90)
class M02ReminderTask implements M02DailyTask {

    static final String NAME = "REMINDERS";

    private final ObjectProvider<ReminderRule> rules;

    M02ReminderTask(ObjectProvider<ReminderRule> rules) {
        this.rules = rules;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean latestDayOnly() {
        return true;
    }

    @Override
    public int run(LocalDate day) {
        return rules.orderedStream().mapToInt(rule -> rule.remind(day)).sum();
    }
}
