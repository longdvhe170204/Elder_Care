package com.eldercare.backend.resident.api.events;

import java.time.Instant;

/** Quá hạn đánh giá lại; 009 nhắc Bác sĩ, mức Trung bình. Khóa sự kiện (residentId, dueAt). */
public record ReassessmentOverdue(long residentId, Instant dueAt) {}
