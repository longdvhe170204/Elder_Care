package com.cambae.backend.m03.internal.allocation.domain;

import java.time.Instant;

/** Bản ghi lịch sử và căn cứ phân bổ nếu có, dùng tính hạn lưu giữ T010. */
public record TimedM03Record(long id, Instant at, Long allocationId) {}
