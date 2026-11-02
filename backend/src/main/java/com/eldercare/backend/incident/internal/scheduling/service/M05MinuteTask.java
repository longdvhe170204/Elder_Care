package com.eldercare.backend.incident.internal.scheduling.service;

import java.time.Instant;

/** Tác vụ chuyển trạng thái, leo thang hoặc nhắc của M05; một lần gọi chỉ xử lý một cấp. */
public interface M05MinuteTask {
    String name();

    int run(Instant scheduledAt, Instant actualAt);
}
