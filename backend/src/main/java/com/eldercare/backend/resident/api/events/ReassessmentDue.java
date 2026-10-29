package com.eldercare.backend.resident.api.events;

/** Cần đánh giá lại, chưa quá hạn; 009 nhắc Bác sĩ, mức Nhẹ (H-14; FR-039 → FR-041). Khóa sự kiện (residentId, noticeId). */
public record ReassessmentDue(long residentId, long noticeId, String basis) {}
