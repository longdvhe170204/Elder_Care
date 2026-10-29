package com.eldercare.backend.notification.internal.job.service;

import java.time.LocalDate;

/** Một việc của job hằng ngày 07:30 ({@code ViecHangNgayM13}; research B-10): nhắc liên hệ trực tiếp còn chờ. */
public interface M13DailyTask {

    String name();

    /** Chạy cho một ngày nghiệp vụ, trả số bản ghi đã xử lý. */
    int run(LocalDate day);
}
