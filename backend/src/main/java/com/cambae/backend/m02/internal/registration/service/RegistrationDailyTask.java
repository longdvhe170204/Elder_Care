package com.cambae.backend.m02.internal.registration.service;

import java.time.LocalDate;

/**
 * Một việc của job khai báo hằng ngày (research Z-06): nhắc quá hạn, nhắc gia hạn, chuyển Hết hạn, nhắc khai báo xóa (cài
 * ở các user story). Việc xét trạng thái tại {@code day}, chạy trong transaction riêng, không lập khai báo (Q-306).
 */
public interface RegistrationDailyTask {

    /** Tên việc trong {@code daily_run}, tối đa 30 ký tự. */
    String name();

    /** Xử lý ngày {@code day}; trả số đối tượng đã xử lý. */
    int run(LocalDate day);
}
