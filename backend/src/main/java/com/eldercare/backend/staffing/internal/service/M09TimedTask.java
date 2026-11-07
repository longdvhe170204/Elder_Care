package com.eldercare.backend.staffing.internal.service;

import java.time.Instant;

/** T008: các story đăng ký việc chuyển trạng thái/nhắc; không dùng để lập bản ghi nghiệp vụ. */
public interface M09TimedTask {
    enum Cadence {
        MINUTE,
        HOURLY,
        DAILY
    }

    Cadence cadence();

    /** Số bản ghi nguồn đã xử lý, để SchedulerRuns ghi kết quả lần chạy. */
    int run(Instant scheduledAt, Instant actualAt);
}
