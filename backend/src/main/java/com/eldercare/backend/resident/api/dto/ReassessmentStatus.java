package com.eldercare.backend.resident.api.dto;

import java.time.Instant;
import java.util.Optional;

/** Trạng thái đánh giá lại của một người cao tuổi (016 FR-044); {@code overdueSince} chỉ có khi {@code OVERDUE}. */
public record ReassessmentStatus(ReassessmentState state, Optional<Instant> dueAt, Optional<Instant> overdueSince) {}
