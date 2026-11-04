package com.eldercare.backend.incident.internal.infection.domain;

import java.time.Instant;

/** Thời gian theo dõi và kết quả loại người cao tuổi khỏi danh sách. */
public record ContactFollowUpContent(Instant from, Instant to, Instant removedAt, String removalReason) {}
