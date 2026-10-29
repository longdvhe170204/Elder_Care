package com.eldercare.backend.resident.api.events;

import java.time.Instant;

/** Tạm vắng quá mốc dự kiến; 009 báo Trưởng tầng, Hành chính, mức Trung bình (FR-050). Khóa sự kiện (absenceId, expectedReturnAt). */
public record TemporaryAbsenceOverdue(long residentId, long absenceId, Instant expectedReturnAt) {}
