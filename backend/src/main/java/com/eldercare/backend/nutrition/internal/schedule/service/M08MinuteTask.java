package com.eldercare.backend.nutrition.internal.schedule.service;

import java.time.Instant;

/**
 * Một việc theo phút của M08 (research M-15): nhắc chốt, bản gán Chờ hiệu lực, món an toàn, suất giữ, giao trễ…
 * Mỗi chức năng đăng ký bean của mình; việc tự mở transaction và tự chống lặp bằng {@link M08JobMarkers} trên dòng nguồn,
 * xét mọi dòng đã đến hạn tại {@code now} (không chỉ phút này) để chạy bù khi bỏ lỡ lần chạy.
 */
public interface M08MinuteTask {

    /** Khóa việc, dùng làm khóa dấu "đã làm" ({@link M08JobMarkers}). */
    String key();

    void run(Instant now);
}
