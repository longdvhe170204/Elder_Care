package com.cambae.backend.m03.internal.allocation.domain;

import java.time.Instant;

/** Chỉ mục công việc báo mốc bắt đầu; không phải lệnh bắt đầu phân bổ. */
public record PastStartAllocation(long id, long residentId, long bedId, Instant startAt, long floorId) {}
