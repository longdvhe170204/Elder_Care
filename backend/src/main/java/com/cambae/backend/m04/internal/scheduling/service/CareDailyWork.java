package com.cambae.backend.m04.internal.scheduling.service;

import java.time.*;
import java.util.List;

/** Story cung cấp ngày còn thiếu từ dữ liệu thật; không tự suy ngày khởi đầu hoặc sinh bản ghi nghiệp vụ. */
public interface CareDailyWork {
    String name();

    List<LocalDate> dueDates(LocalDate today);

    int run(LocalDate date, Instant now);
}
