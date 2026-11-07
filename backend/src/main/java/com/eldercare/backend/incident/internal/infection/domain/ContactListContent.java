package com.eldercare.backend.incident.internal.infection.domain;

import java.time.Instant;

/** Khoảng truy vết và thông tin xác nhận của danh sách tiếp xúc. */
public record ContactListContent(
        Instant traceFrom, Instant traceTo, Instant confirmedAt, Long confirmedBy, String noContactsReason) {}
