package com.eldercare.backend.care.internal.scheduling.service;

import java.time.Instant;

/** Bước chuyển trạng thái/nhắc sẽ đăng ký dần ở story; tự truy vấn mọi mốc đã lỡ, không tạo công việc. */
public interface CareMinuteWork {
    int run(Instant now);
}
