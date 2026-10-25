package com.cambae.backend.m02.internal.transition.service;

import java.time.LocalDate;

/**
 * Một loại nhắc Hành chính của việc hằng ngày {@code REMINDERS} (FR-017, FR-021, FR-026, FR-029, FR-066a, FR-072,
 * FR-076). Mỗi user story thêm bean cài giao diện này; {@link M02ReminderTask} chạy lần lượt các bean.
 */
public interface ReminderRule {

    /**
     * Gửi các lần nhắc của ngày {@code day} (qua sự kiện {@code M02Reminder}, mức Nhẹ), ghi vào {@link ReminderLog}.
     *
     * @return số lần nhắc đã gửi
     */
    int remind(LocalDate day);
}
