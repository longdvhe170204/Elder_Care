package com.eldercare.backend.health.internal.scheduling.service;

import java.time.Instant;

/** Tác vụ chuyển trạng thái hoặc nhắc của M06; mỗi lần gọi chỉ xử lý một cấp trạng thái. */
public interface M06TimedTask {
    enum Cadence {
        MINUTE,
        DAILY
    }

    String name();

    Cadence cadence();

    int run(Instant scheduledAt, Instant actualAt);
}
