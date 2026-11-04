package com.eldercare.backend.family.internal.scheduling.service;

import java.time.Instant;

/**
 * Tác vụ chuyển trạng thái, nhắc của M10 (research F-17), đăng ký ở story tương ứng; không lập bản ghi nghiệp vụ (Q-306).
 * Mỗi (tác vụ, mốc) chạy đúng một lần; tác vụ theo phút, theo giờ xử lý mọi việc đã đến hạn tới {@code actualAt}.
 */
public interface M10TimedTask {

    enum Cadence {
        MINUTE,
        HOURLY,
        MIDNIGHT
    }

    /** Tên ổn định, dùng làm khóa của dấu đã chạy. */
    String name();

    Cadence cadence();

    /** Số bản ghi đã xử lý. */
    int run(Instant scheduledAt, Instant actualAt);
}
