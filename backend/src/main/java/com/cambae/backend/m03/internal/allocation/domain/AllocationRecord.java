package com.cambae.backend.m03.internal.allocation.domain;

import java.time.Instant;

/** Ảnh khóa của lệnh phân bổ; không lộ entity JPA ra web. */
public record AllocationRecord(
        long id, long residentId, long bedId, AllocationStatus status, Instant startAt, Instant expectedEnd) {}
