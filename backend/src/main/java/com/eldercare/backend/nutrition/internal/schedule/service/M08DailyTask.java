package com.eldercare.backend.nutrition.internal.schedule.service;

import java.time.Instant;
import java.time.LocalDate;

/** Một việc hằng ngày của M08 (thực đơn Đã áp dụng, nhắc CFG-M08-07…); quy tắc như {@link M08MinuteTask}. */
public interface M08DailyTask {

    String key();

    void run(LocalDate today, Instant now);
}
