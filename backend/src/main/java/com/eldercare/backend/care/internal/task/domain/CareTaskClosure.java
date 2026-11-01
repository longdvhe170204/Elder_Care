package com.eldercare.backend.care.internal.task.domain;

import java.time.Instant;

/** Thông tin đóng đã lưu, đọc riêng để không giả dữ liệu khi chưa có kết quả ghi nhận. */
public record CareTaskClosure(String reason, String rule, Instant at) {}
