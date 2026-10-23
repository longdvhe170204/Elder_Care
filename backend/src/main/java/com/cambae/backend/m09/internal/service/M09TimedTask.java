package com.cambae.backend.m09.internal.service;

import java.time.Instant;

/** T008: các story đăng ký việc chuyển trạng thái/nhắc; không dùng để lập bản ghi nghiệp vụ. */
public interface M09TimedTask {
    enum Cadence {
        MINUTE,
        HOURLY,
        DAILY
    }

    Cadence cadence();

    void run(Instant scheduledAt, Instant actualAt);
}
